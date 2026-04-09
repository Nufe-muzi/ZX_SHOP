import json
import os

CONFIG_PATH = "rag_config.json"

DEFAULT_CONFIG = {
    "embedding_model": "text-embedding-3-small",
    "chunk_size": 500,
    "chunk_overlap": 50,
    "chunk_mode": "recursive",
    "search_type": "hybrid",
    "weight_semantic": 0.7,
    "use_rerank": True,
    "rerank_model": "gpt-4o-mini",
    "index_path": "faiss_index"
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
