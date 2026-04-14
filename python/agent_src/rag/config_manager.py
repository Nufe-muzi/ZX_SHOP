import json
import os

CONFIG_PATH = "rag_config.json"

DEFAULT_CONFIG = {
    "embedding_model": "text-embedding-v2",
    "chunk_size": 500,
    "chunk_overlap": 80,
    "chunk_mode": "recursive",
    "search_type": "hybrid",
    "weight_semantic": 0.5,
    "use_rerank": False,
    "rerank_model": "qwen-plus-2025-07-28",
    "index_path": "faiss_index",
    "retriever_k": 10,
    "bm25_k": 10,
    "separator_type": "newline"
}

def load_config():
    if os.path.exists(CONFIG_PATH):
        try:
            with open(CONFIG_PATH, "r", encoding="utf-8") as f:
                return {**DEFAULT_CONFIG, **json.load(f)}
        except Exception as e:
            print(f"加载配置失败: {e}")
    return DEFAULT_CONFIG.copy()

def save_config(config_data):
    with open(CONFIG_PATH, "w", encoding="utf-8") as f:
        json.dump(config_data, f, indent=4, ensure_ascii=False)
