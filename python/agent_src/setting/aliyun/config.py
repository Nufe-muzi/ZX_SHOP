import os

class AliyunConfig:
    """
    阿里云 (DashScope) 大语言模型与向量模型配置管理
    """
    def __init__(self):
        # 请确保在环境变量中设置了 DASHSCOPE_API_KEY
        self.api_key = os.getenv("DASHSCOPE_API_KEY", "sk-2b8ddae4c7b7482cadf5740a6df6ce1e")
        self.base_url = os.getenv("DASHSCOPE_BASE_URL", "https://dashscope.aliyuncs.com/compatible-mode/v1")
        
        # LLM (大语言模型) 默认配置
        self.llm_model = os.getenv("ALIYUN_LLM_MODEL", "qwen-plus-2025-07-28") # 也可选择 qwen-plus, qwen-turbo 等
        
        # 向量 (Embedding) 模型默认配置
        self.embedding_model = os.getenv("ALIYUN_EMBEDDING_MODEL", "text-embedding-v2") # 获取向量表示的模型
        
    def get_llm_kwargs(self):
        """
        获取传递给 LLM 接口的参数
        """
        return {
            "api_key": self.api_key,
            "base_url": self.base_url,
            "model": self.llm_model,
        }
        
    def get_embedding_kwargs(self):
        """
        获取传递给 Embedding 接口的参数
        """
        return {
            "api_key": self.api_key,
            "base_url": self.base_url,
            "model": self.embedding_model,
        }

# 实例化一个默认配置对象供其他模块导入和使用
aliyun_config = AliyunConfig()
