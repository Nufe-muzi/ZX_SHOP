import os
from langchain_community.document_loaders import TextLoader, DirectoryLoader
from langchain_text_splitters import RecursiveCharacterTextSplitter, CharacterTextSplitter
from langchain_community.vectorstores import FAISS
from langchain_community.retrievers import BM25Retriever
from langchain_classic.retrievers import EnsembleRetriever
from langchain_classic.retrievers.contextual_compression import ContextualCompressionRetriever
from langchain_openai import ChatOpenAI
from langchain_classic.retrievers.document_compressors import LLMChainExtractor
from agent_src.rag.config_manager import load_config
from agent_src.setting.aliyun.config import aliyun_config
from typing import List
from langchain_core.embeddings import Embeddings


class DashScopeEmbeddings(Embeddings):
    """阿里云 DashScope Embedding 封装类"""

    def __init__(self, api_key: str, model: str = "text-embedding-v2"):
        self.api_key = api_key
        self.model = model

    def embed_documents(self, texts: List[str]) -> List[List[float]]:
        """批量向量化文档"""
        import dashscope
        from dashscope import TextEmbedding

        dashscope.api_key = self.api_key

        # DashScope 要求文本列表格式
        result = TextEmbedding.call(
            model=self.model,
            input=texts
        )

        if result.status_code != 200:
            raise ValueError(f"Embedding API 调用失败: {result.message}")

        # 按原始顺序返回向量
        embeddings = [item['embedding'] for item in result.output['embeddings']]
        return embeddings

    def embed_query(self, text: str) -> List[float]:
        """向量化查询文本"""
        return self.embed_documents([text])[0]

class RAGManager:
    def __init__(self, index_path=None):
        config = load_config()
        self.config = config
        embedding_model = config.get("embedding_model") or aliyun_config.embedding_model
        self.embeddings = DashScopeEmbeddings(
            api_key=aliyun_config.api_key,
            model=embedding_model
        )
        self.index_path = index_path or config["index_path"]
        self.vectorstore = None
        self.bm25_retriever = None  # BM25 检索器
        self.all_documents = []  # 存储所有文档用于 BM25
        llm_kwargs = aliyun_config.get_llm_kwargs().copy()
        llm_kwargs["model"] = config.get("rerank_model") or llm_kwargs.get("model")
        self.llm = ChatOpenAI(**llm_kwargs, temperature=0)

        if os.path.exists(self.index_path):
            self.vectorstore = FAISS.load_local(self.index_path, self.embeddings, allow_dangerous_deserialization=True)
            # 从 FAISS 重建文档列表用于 BM25
            self._rebuild_documents_from_faiss()

    def refresh_config(self):
        """重新加载最新配置"""
        self.config = load_config()
        embedding_model = self.config.get("embedding_model") or aliyun_config.embedding_model
        self.embeddings = DashScopeEmbeddings(
            api_key=aliyun_config.api_key,
            model=embedding_model
        )
        llm_kwargs = aliyun_config.get_llm_kwargs().copy()
        llm_kwargs["model"] = self.config.get("rerank_model") or llm_kwargs.get("model")
        self.llm = ChatOpenAI(**llm_kwargs, temperature=0)

    def _rebuild_documents_from_faiss(self):
        """从 FAISS 重建文档列表用于 BM25"""
        from langchain_core.documents import Document
        try:
            # 从 FAISS docstore 获取所有文档
            doc_dict = self.vectorstore.docstore._dict
            self.all_documents = [
                Document(page_content=doc.page_content, metadata=doc.metadata)
                for doc in doc_dict.values()
                if hasattr(doc, 'page_content')
            ]
            if self.all_documents:
                self.bm25_retriever = BM25Retriever.from_documents(self.all_documents)
                self.bm25_retriever.k = self.config.get("bm25_k", 10)
        except Exception as e:
            print(f"重建 BM25 索引失败: {e}")
            self.bm25_retriever = None

    def get_splitter(self, mode=None, chunk_size=None, chunk_overlap=None, separators=None):
        """获取切分器，优先使用传入参数，否则使用全局配置

        Dify 风格的默认分隔符（按优先级排序）：
        1. \\n\\n - 段落分隔
        2. \\n - 行分隔
        3. 。 - 中文句号
        4. ！ - 中文感叹号
        5. ？ - 中文问号
        6. ； - 中文分号
        7. . - 英文句号
        8. ! - 英文感叹号
        9. ? - 英文问号
        10. ; - 英文分号
        11. 空格 - 最后兜底
        """
        mode = mode or self.config["chunk_mode"]
        chunk_size = chunk_size or self.config["chunk_size"]
        chunk_overlap = chunk_overlap or self.config["chunk_overlap"]

        # Dify 默认分隔符（按优先级排序）
        default_separators = [
            "\n\n",     # 段落分隔
            "\n",       # 行分隔
            "。",       # 中文句号
            "！",       # 中文感叹号
            "？",       # 中文问号
            "；",       # 中文分号
            "……",      # 中文省略号
            ".",        # 英文句号
            "!",        # 英文感叹号
            "?",        # 英文问号
            ";",        # 英文分号
            " ",        # 空格
            ""          # 最后兜底，按字符切分
        ]

        separators = separators or default_separators

        if mode == "recursive":
            return RecursiveCharacterTextSplitter(
                chunk_size=chunk_size,
                chunk_overlap=chunk_overlap,
                separators=separators,
                length_function=len,
                is_separator_regex=False
            )
        else:
            return CharacterTextSplitter(
                chunk_size=chunk_size,
                chunk_overlap=chunk_overlap,
                separator="\n\n"  # 简单模式使用段落分隔
            )

    def get_advanced_retriever(self, search_type=None, weight_semantic=None, use_rerank=None, k=None):
        """获取高级检索器 (混合检索 + 重排序)"""
        search_type = search_type or self.config["search_type"]
        weight_semantic = weight_semantic or self.config["weight_semantic"]
        use_rerank = use_rerank if use_rerank is not None else self.config["use_rerank"]
        k = k or self.config.get("retriever_k", 10)  # 默认召回 10 条

        if self.vectorstore is None:
            raise ValueError("向量库未初始化")

        # 1. 语义检索器 (增大召回数量)
        vector_retriever = self.vectorstore.as_retriever(search_kwargs={"k": k})

        if search_type == "hybrid" and self.bm25_retriever is not None:
            # 2. 混合检索：语义 + BM25 关键词
            self.bm25_retriever.k = k
            retriever = EnsembleRetriever(
                retrievers=[vector_retriever, self.bm25_retriever],
                weights=[weight_semantic, 1 - weight_semantic]
            )
        else:
            retriever = vector_retriever

        # 3. 重排序 (Rerank) - 使用 LLMChainExtractor
        if use_rerank:
            compressor = LLMChainExtractor.from_llm(self.llm)
            retriever = ContextualCompressionRetriever(
                base_compressor=compressor, base_retriever=retriever
            )

        return retriever

    def preview_documents(self, file_path, mode=None, chunk_size=None, chunk_overlap=None, separators=None):
        """预览文档切分结果，返回切分后的文本列表

        返回格式包含分隔符标记，方便用户了解切分边界
        """
        # 尝试多种编码读取文件
        encodings = ["utf-8", "gbk", "gb2312", "utf-16", "latin-1"]
        documents = None
        used_encoding = None

        for encoding in encodings:
            try:
                loader = TextLoader(file_path, encoding=encoding)
                documents = loader.load()
                used_encoding = encoding
                break
            except (UnicodeDecodeError, UnicodeError):
                continue

        if documents is None:
            raise ValueError(f"无法读取文件，尝试了编码: {encodings}")

        # 规范化换行符：将 \r\n 和 \r 统一转换为 \n
        for doc in documents:
            if doc.page_content:
                # 显示原始内容的换行符信息（调试用）
                original = doc.page_content
                has_cr = '\r' in original
                has_lf = '\n' in original
                print(f"文件编码: {used_encoding}, 包含\\r: {has_cr}, 包含\\n: {has_lf}")

                # 规范化换行符
                doc.page_content = original.replace('\r\n', '\n').replace('\r', '\n')

        splitter = self.get_splitter(mode, chunk_size, chunk_overlap, separators)
        splits = splitter.split_documents(documents)

        # 返回带分隔符标记的预览
        preview_results = []
        for i, split in enumerate(splits):
            content = split.page_content
            # 添加分隔符标记
            preview_results.append({
                "index": i + 1,
                "content": content,
                "char_count": len(content),
                "separator_hint": self._detect_separator(content)
            })

        return preview_results

    def _detect_separator(self, text):
        """检测文本片段末尾的分隔符类型"""
        if not text:
            return "无"

        last_chars = text[-10:] if len(text) >= 10 else text

        if "\n\n" in last_chars:
            return "段落分隔(\\n\\n)"
        elif "\n" in last_chars:
            return "换行分隔(\\n)"
        elif "。" in last_chars:
            return "中文句号(。)"
        elif "！" in last_chars or "!" in last_chars:
            return "感叹号"
        elif "？" in last_chars or "?" in last_chars:
            return "问号"
        elif "；" in last_chars or ";" in last_chars:
            return "分号"
        elif "." in last_chars:
            return "英文句号(.)"
        else:
            return "字符边界"

    def ingest_documents(self, documents):
        """将文档列表向量化并存入 FAISS 索引"""
        if not documents:
            return 0

        try:
            # 过滤空文档，确保内容是有效字符串
            valid_docs = []
            for doc in documents:
                content = doc.page_content
                if content and isinstance(content, str) and content.strip():
                    valid_docs.append(doc)

            if not valid_docs:
                raise ValueError("没有有效的文档内容可供向量化")

            # 提取纯文本列表
            texts = [doc.page_content for doc in valid_docs]
            metadatas = [doc.metadata for doc in valid_docs]

            print(f"准备向量化 {len(texts)} 个文本片段...")
            print(f"第一个片段预览: {texts[0][:100]}...")

            if self.vectorstore is None:
                self.vectorstore = FAISS.from_texts(texts, self.embeddings, metadatas=metadatas)
            else:
                self.vectorstore.add_texts(texts, metadatas=metadatas)

            # 更新文档列表和 BM25 索引
            self.all_documents.extend(valid_docs)
            self.bm25_retriever = BM25Retriever.from_documents(self.all_documents)
            self.bm25_retriever.k = self.config.get("bm25_k", 10)

            # 确保索引目录存在
            os.makedirs(self.index_path, exist_ok=True)

            # 保存索引
            self.vectorstore.save_local(self.index_path)
            return len(valid_docs)
        except Exception as e:
            print(f"向量化入库错误: {e}")
            import traceback
            traceback.print_exc()
            raise

    def ingest_directory(self, directory_path):
        """将目录下的所有文档向量化入库"""
        loader = DirectoryLoader(directory_path, glob="**/*.txt")
        documents = loader.load()
        splitter = self.get_splitter()
        splits = splitter.split_documents(documents)
        return self.ingest_documents(splits)

    def search(self, query, k=None):
        """执行相似度检索，返回结果和召回率信息"""
        if self.vectorstore is None:
            return [], {"total_docs": 0, "retrieved": 0, "recall_rate": 0}

        k = k or self.config.get("retriever_k", 10)

        # 使用带分数的相似度检索
        docs_with_scores = self.vectorstore.similarity_search_with_score(query, k=k)

        # 构建结果列表，包含分数
        results = []
        for doc, score in docs_with_scores:
            # FAISS 使用余弦距离 (L2 归一化后的内积距离)
            # distance 范围: 0-2 (0=完全相同, 2=完全相反)
            # 余弦相似度 = 1 - distance，范围: -1 到 1
            # Dify 风格: 归一化到 0-1 范围
            score_float = float(score)
            cosine_similarity = 1 - score_float  # 转换为余弦相似度
            # 归一化到 0-1 范围: (-1, 1) -> (0, 1)
            normalized_score = (cosine_similarity + 1) / 2

            results.append({
                "content": doc.page_content,
                "metadata": doc.metadata,
                "score": round(normalized_score, 4),  # 0-1 范围，Dify 风格
                "score_percent": round(normalized_score * 100, 2),  # 百分比形式
                "distance": round(score_float, 4)
            })

        # 按分数排序（高分在前）
        results.sort(key=lambda x: x["score"], reverse=True)

        # 计算召回率信息
        total_docs = len(self.all_documents)
        retrieved = len(results)
        recall_rate = retrieved / total_docs if total_docs > 0 else 0

        recall_info = {
            "total_docs": total_docs,
            "retrieved": retrieved,
            "recall_rate": round(recall_rate * 100, 2)
        }

        return results, recall_info

