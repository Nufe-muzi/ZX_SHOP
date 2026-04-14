from fastapi import FastAPI, UploadFile, File, Form, Body, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from typing import List, Optional, Dict
from agent_src.rag.rag_store import RAGManager
from agent_src.rag.config_manager import load_config, save_config
from agent_src.rag.document_loaders import get_supported_formats
from agent_src.rag.file_processor import FileProcessor
from langchain_core.documents import Document
import os
import shutil
import traceback
import json

app = FastAPI()

# 添加 CORS 中间件，允许跨域请求
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 允许所有来源
    allow_credentials=True,
    allow_methods=["*"],  # 允许所有方法
    allow_headers=["*"],  # 允许所有请求头
)

rag_manager = RAGManager()

# 预览缓存
preview_cache: Dict[str, Dict] = {}

# 文件处理器
file_processor = FileProcessor()

# 知识库管理器
from agent_src.rag.database import kb_manager, init_db


@app.on_event("startup")
async def startup_event():
    """应用启动时初始化数据库"""
    try:
        await init_db()
        print("数据库初始化完成")
    except Exception as e:
        print(f"数据库初始化失败: {e}")

# --- 知识库管理接口 ---

@app.post("/rag/kb/create")
async def create_knowledge_base(config: Dict = Body(...)):
    """创建知识库"""
    try:
        kb = await kb_manager.create_kb(
            name=config.get("name"),
            description=config.get("description", ""),
            embedding_model=config.get("embedding_model", "text-embedding-v2"),
            chunk_size=config.get("chunk_size", 800),
            chunk_overlap=config.get("chunk_overlap", 80),
            separator_type=config.get("separator_type", "newline")
        )
        return {"status": "success", "kb": kb}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"创建知识库失败: {str(e)}")

@app.get("/rag/kb/list")
async def list_knowledge_bases():
    """列出所有知识库"""
    try:
        kbs = await kb_manager.list_kbs()
        return {"kbs": kbs}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"获取知识库列表失败: {str(e)}")

@app.get("/rag/kb/{kb_id}")
async def get_knowledge_base(kb_id: int):
    """获取知识库详情"""
    kb = await kb_manager.get_kb(kb_id)
    if not kb:
        raise HTTPException(status_code=404, detail="知识库不存在")
    return kb

@app.delete("/rag/kb/{kb_id}")
async def delete_knowledge_base(kb_id: int):
    """删除知识库"""
    try:
        success = await kb_manager.delete_kb(kb_id)
        if not success:
            raise HTTPException(status_code=404, detail="知识库不存在")
        return {"status": "success", "msg": "知识库已删除"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"删除知识库失败: {str(e)}")

@app.get("/rag/kb/{kb_id}/documents")
async def list_kb_documents(kb_id: int):
    """列出知识库中的文档"""
    try:
        documents = await kb_manager.list_documents(kb_id)
        return {"documents": documents}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"获取文档列表失败: {str(e)}")

@app.get("/rag/document/{doc_id}")
async def get_document(doc_id: int):
    """获取文档详情"""
    doc = await kb_manager.get_document(doc_id)
    if not doc:
        raise HTTPException(status_code=404, detail="文档不存在")
    return doc

@app.get("/rag/document/{doc_id}/chunks")
async def get_document_chunks(doc_id: int):
    """获取文档的所有片段"""
    try:
        chunks = await kb_manager.get_document_chunks(doc_id)
        return {"chunks": chunks}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"获取片段失败: {str(e)}")

@app.delete("/rag/document/{doc_id}")
async def delete_document(doc_id: int):
    """删除文档"""
    try:
        success = await kb_manager.delete_document(doc_id)
        if not success:
            raise HTTPException(status_code=404, detail="文档不存在")
        return {"status": "success", "msg": "文档已删除"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"删除文档失败: {str(e)}")

# --- 后台管理配置接口 ---

@app.get("/admin/rag-config")
async def get_system_config():
    """获取当前的 RAG 系统全局配置"""
    return load_config()

@app.post("/admin/rag-config/update")
async def update_system_config(config: Dict = Body(...)):
    """更新 RAG 系统配置并实时生效"""
    # 1. 保存到 JSON 文件
    current_config = load_config()
    new_config = {**current_config, **config}
    save_config(new_config)
    
    # 2. 通知 RAG 管理器热重新加载
    rag_manager.refresh_config()
    
    return {"status": "success", "msg": "配置已更新，即时生效", "config": new_config}

# --- 原有 RAG 业务接口 ---

@app.get("/rag/supported-formats")
async def get_supported_file_formats():
    """获取支持的文件格式列表"""
    return {
        "formats": get_supported_formats(),
        "descriptions": {
            ".txt": "纯文本文件",
            ".pdf": "PDF 文档",
            ".docx": "Word 文档 (2007+)",
            ".doc": "Word 文档 (旧版)",
            ".xlsx": "Excel 表格 (2007+)",
            ".xls": "Excel 表格 (旧版)",
            ".csv": "CSV 表格",
            ".md": "Markdown 文档",
            ".markdown": "Markdown 文档",
            ".html": "HTML 网页",
            ".htm": "HTML 网页",
            ".json": "JSON 数据"
        }
    }

@app.post("/rag/preview")
async def preview_upload(
    file: UploadFile = File(...),
    mode: Optional[str] = Form("recursive"),
    chunk_size: Optional[int] = Form(800),
    chunk_overlap_percent: Optional[int] = Form(10),
    separator_type: Optional[str] = Form("newline"),
    separators: Optional[str] = Form(None),
    clean_settings: Optional[str] = Form(None)
):
    """
    预览接口：支持多种文件格式，返回带分隔符标记的切分结果

    Args:
        file: 上传的文件
        mode: 切分模式 (recursive/character)
        chunk_size: 分段最大长度
        chunk_overlap_percent: 分段重叠度百分比
        separator_type: 分隔符类型 (newline/paragraph/sentence/space/custom)
        separators: 自定义分隔符（每行一个）
        clean_settings: 清洗设置 (JSON字符串)
    """
    import json

    # 检查文件格式
    ext = os.path.splitext(file.filename)[1].lower()
    supported = get_supported_formats()

    if ext not in supported:
        raise HTTPException(
            status_code=400,
            detail=f"不支持的文件格式: {ext}。支持的格式: {supported}"
        )

    # 计算重叠字符数
    chunk_overlap = int(chunk_size * chunk_overlap_percent / 100)

    # 解析自定义分隔符
    custom_separators = None
    if separator_type == "custom" and separators and separators.strip():
        custom_separators = []
        for line in separators.strip().split('\n'):
            sep = line.strip()
            if sep:
                sep = sep.replace('\\n', '\n').replace('\\t', '\t').replace('\\r', '\r')
                custom_separators.append(sep)
        if custom_separators and '' not in custom_separators:
            custom_separators.append('')

    # 解析清洗设置
    clean_opts = {}
    if clean_settings:
        try:
            clean_opts = json.loads(clean_settings)
        except:
            pass

    # 更新文件处理器设置
    file_processor.update_settings(
        chunk_size=chunk_size,
        chunk_overlap=chunk_overlap,
        separator_type=separator_type,
        custom_separators=custom_separators,
        clean_settings=clean_opts
    )

    # 保存临时文件（保留原始扩展名）
    import tempfile
    ext = os.path.splitext(file.filename)[1].lower()
    with tempfile.NamedTemporaryFile(delete=False, suffix=ext) as tmp:
        shutil.copyfileobj(file.file, tmp)
        temp_path = tmp.name

    try:
        # 使用文件处理器预览
        preview_result = file_processor.preview_file(temp_path, mode)

        # 缓存预览内容（包含完整信息）
        preview_cache[file.filename] = {
            "chunks": preview_result["chunks"],
            "total_chars": preview_result["total_chars"],
            "clean_settings": clean_opts,
            "separator_type": separator_type,
            "chunk_size": chunk_size,
            "chunk_overlap": chunk_overlap
        }

        return {
            "filename": file.filename,
            "file_type": ext,
            "mode": mode,
            "chunk_size": chunk_size,
            "chunk_overlap": chunk_overlap,
            "chunk_count": preview_result["chunk_count"],
            "total_chars": preview_result["total_chars"],
            "previews": preview_result["chunks"][:10],  # 返回前10条预览
            "separator_type": separator_type
        }
    except Exception as e:
        traceback.print_exc()  # 打印完整错误堆栈
        raise HTTPException(status_code=500, detail=f"处理文件失败: {str(e)}")
    finally:
        # 清理临时文件
        if os.path.exists(temp_path):
            os.remove(temp_path)

@app.post("/rag/commit")
async def commit_to_vectorstore(
    filename: str = Form(...),
    kb_id: int = Form(None),
):
    """
    2. 确定入库向量化接口
    支持指定知识库ID，将文档保存到数据库
    """
    if filename not in preview_cache:
        raise HTTPException(status_code=400, detail="请先调用预览接口")

    cached_data = preview_cache[filename]
    chunks = cached_data["chunks"]

    if not chunks:
        raise HTTPException(status_code=400, detail="没有可入库的内容")

    try:
        # 如果指定了知识库ID，保存到数据库
        if kb_id:
            # 准备片段数据
            chunk_data = [
                {
                    "index": i,
                    "content": c["content"],
                    "char_count": c.get("char_count", len(c["content"])),
                    "separator_hint": c.get("separator_hint", ""),
                    "metadata": {}
                }
                for i, c in enumerate(chunks)
            ]

            # 保存到数据库
            doc = await kb_manager.add_document(
                kb_id=kb_id,
                filename=filename,
                file_type=os.path.splitext(filename)[1].lower(),
                file_size=cached_data.get("total_chars", 0),
                total_chars=cached_data["total_chars"],
                chunk_count=len(chunks),
                clean_settings=cached_data.get("clean_settings", {}),
                chunks=chunk_data
            )

            # 同时向量化
            splits = [Document(page_content=c["content"], metadata={"source": filename, "kb_id": kb_id}) for c in chunks]
            count = rag_manager.ingest_documents(splits)

            del preview_cache[filename]

            return {
                "status": "success",
                "ingested_count": count,
                "doc_id": doc.get("id"),
                "msg": f"文件 {filename} 已保存到知识库并完成向量化。"
            }
        else:
            # 不指定知识库，只向量化
            splits = [Document(page_content=c["content"], metadata={"source": filename}) for c in chunks]
            count = rag_manager.ingest_documents(splits)
            del preview_cache[filename]

            return {
                "status": "success",
                "ingested_count": count,
                "msg": f"文件 {filename} 已完成向量化并存入库中。"
            }
    except Exception as e:
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=f"向量化入库失败: {str(e)}")

@app.get("/rag/config")
async def get_rag_config():
    """获取当前 RAG 配置"""
    return {
        "embedding_model": rag_manager.embeddings.model,
        "index_path": rag_manager.index_path
    }

@app.post("/rag/search")
async def search_knowledge(request: Dict = Body(...)):
    """
    知识检索接口
    支持指定知识库ID进行检索
    """
    query = request.get("query", "")
    k = request.get("k", 10)
    kb_id = request.get("kb_id")

    if not query:
        raise HTTPException(status_code=400, detail="query 参数不能为空")

    # 如果指定了知识库ID，从数据库获取片段进行检索
    if kb_id:
        try:
            # 获取知识库所有片段
            all_chunks = await kb_manager.get_all_chunks(kb_id)

            if not all_chunks:
                return {
                    "query": query,
                    "result_count": 0,
                    "recall_info": {"total_docs": 0, "retrieved": 0, "recall_rate": "0%"},
                    "results": []
                }

            # 使用向量检索
            if rag_manager.vectorstore is None:
                # 如果向量库未初始化，先构建
                docs = [
                    Document(page_content=c["content"], metadata={"source": c["filename"], "chunk_id": c["id"]})
                    for c in all_chunks
                ]
                rag_manager.ingest_documents(docs)

            results, recall_info = rag_manager.search(query, k=k)

            return {
                "query": query,
                "result_count": len(results),
                "recall_info": recall_info,
                "results": results
            }
        except Exception as e:
            traceback.print_exc()
            raise HTTPException(status_code=500, detail=f"检索失败: {str(e)}")
    else:
        # 原有逻辑
        if rag_manager.vectorstore is None:
            return {"error": "向量库未初始化，请先上传文档"}

        results, recall_info = rag_manager.search(query, k=k)
        return {
            "query": query,
            "result_count": len(results),
            "recall_info": recall_info,
            "results": results
        }

@app.post("/workflow/run")
async def run_workflow(question: str = Body(..., embed=True)):
    """
    运行完整工作流 (RAG + LLM)
    """
    from agent_src.graph.workflow import workflow_app

    inputs = {"question": question}
    outputs = []

    for output in workflow_app.stream(inputs):
        for key, value in output.items():
            outputs.append({"node": key, "output": value})

    return {
        "question": question,
        "workflow_outputs": outputs
    }
