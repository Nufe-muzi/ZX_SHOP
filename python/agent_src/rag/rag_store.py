import os
from langchain_community.document_loaders import TextLoader, DirectoryLoader
from langchain_text_splitters import RecursiveCharacterTextSplitter, CharacterTextSplitter
from langchain_openai import OpenAIEmbeddings
from langchain_community.vectorstores import FAISS
from langchain_community.retrievers import BM25Retriever
from langchain.retrievers import EnsembleRetriever
from langchain.retrievers.contextual_compression import ContextualCompressionRetriever
from langchain_openai import ChatOpenAI
from langchain.retrievers.document_compressors import LLMChainExtractor
from agent_src.rag.config_manager import load_config

class RAGManager:
    def __init__(self, index_path=None):
        config = load_config()
        self.config = config
        self.embeddings = OpenAIEmbeddings(model=config["embedding_model"])
        self.index_path = index_path or config["index_path"]
        self.vectorstore = None
        self.llm = ChatOpenAI(model=config["rerank_model"], temperature=0)
        
        if os.path.exists(self.index_path):
            self.vectorstore = FAISS.load_local(self.index_path, self.embeddings, allow_dangerous_deserialization=True)

    def refresh_config(self):
        """重新加载最新配置"""
        self.config = load_config()
        self.embeddings = OpenAIEmbeddings(model=self.config["embedding_model"])
        self.llm = ChatOpenAI(model=self.config["rerank_model"], temperature=0)

    def get_splitter(self, mode=None, chunk_size=None, chunk_overlap=None):
        """获取切分器，优先使用传入参数，否则使用全局配置"""
        mode = mode or self.config["chunk_mode"]
        chunk_size = chunk_size or self.config["chunk_size"]
        chunk_overlap = chunk_overlap or self.config["chunk_overlap"]
        
        if mode == "recursive":
            return RecursiveCharacterTextSplitter(chunk_size=chunk_size, chunk_overlap=chunk_overlap)
        else:
            return CharacterTextSplitter(chunk_size=chunk_size, chunk_overlap=chunk_overlap)

    def get_advanced_retriever(self, search_type=None, weight_semantic=None, use_rerank=None):
        """获取高级检索器 (混合检索 + 重排序)"""
        search_type = search_type or self.config["search_type"]
        weight_semantic = weight_semantic or self.config["weight_semantic"]
        use_rerank = use_rerank if use_rerank is not None else self.config["use_rerank"]

        if self.vectorstore is None:
            raise ValueError("向量库未初始化")

        # 1. 语义检索器
        vector_retriever = self.vectorstore.as_retriever(search_kwargs={"k": 5})

        if search_type == "hybrid":
            # 2. 关键词检索器 (BM25需要先构建全量文档库，这里简化演示)
            # 实际生产中建议在 ingest 时同步构建 BM25 索引
            # docs = self.vectorstore.docstore._dict.values() # 仅为示例
            # bm25_retriever = BM25Retriever.from_documents(list(docs))
            # bm25_retriever.k = 5
            
            # 3. 混合检索
            # retriever = EnsembleRetriever(
            #     retrievers=[vector_retriever, bm25_retriever],
            #     weights=[weight_semantic, 1 - weight_semantic]
            # )
            retriever = vector_retriever # 暂时回退到语义，演示框架
        else:
            retriever = vector_retriever

        # 4. 重排序 (Rerank) - 使用 LLMChainExtractor 或 BGE Reranker 等
        if use_rerank:
            compressor = LLMChainExtractor.from_llm(self.llm)
            retriever = ContextualCompressionRetriever(
                base_compressor=compressor, base_retriever=retriever
            )

        return retriever
