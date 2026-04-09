from fastapi import FastAPI, UploadFile, File, Form, Body
from typing import List, Optional, Dict
from agent_src.rag.rag_store import RAGManager
from agent_src.rag.config_manager import load_config, save_config
from langchain.docstore.document import Document
import os
import shutil

app = FastAPI()
rag_manager = RAGManager()

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

@app.post("/rag/preview")
async def preview_upload(
    file: UploadFile = File(...),
    mode: Optional[str] = Form(None),
    chunk_size: Optional[int] = Form(None),
    chunk_overlap: Optional[int] = Form(None)
):
    """
    1. 预览接口：支持临时覆盖全局参数
    """
    # 保存临时文件
    temp_path = f"temp_{file.filename}"
    with open(temp_path, "wb") as buffer:
        shutil.copyfileobj(file.file, buffer)
    
    # 预览切分
    previews = rag_manager.preview_documents(temp_path, mode, chunk_size, chunk_overlap)
    
    # 缓存预览，以便确认入库
    preview_cache[file.filename] = previews
    
    # 清理临时文件
    os.remove(temp_path)
    
    return {
        "filename": file.filename,
        "mode": mode,
        "chunk_size": chunk_size,
        "chunk_count": len(previews),
        "previews": previews[:3]  # 返回前3条预览
    }

@app.post("/rag/commit")
async def commit_to_vectorstore(
    filename: str = Form(...),
):
    """
    2. 确定入库向量化接口
    """
    if filename not in preview_cache:
        return {"error": "请先调用预览接口"}
        
    previews = preview_cache[filename]
    # 将文本封装回 Document 对象
    splits = [Document(page_content=text, metadata={"source": filename}) for text in previews]
    
    count = rag_manager.ingest_documents(splits)
    del preview_cache[filename]
    
    return {
        "status": "success",
        "ingested_count": count,
        "msg": f"文件 {filename} 已完成向量化并存入库中。"
    }

@app.get("/rag/config")
async def get_rag_config():
    """获取当前 RAG 配置"""
    return {
        "embedding_model": rag_manager.embeddings.model,
        "index_path": rag_manager.index_path
    }
