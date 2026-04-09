import os
from agent_src.rag.rag_store import RAGManager
from agent_src.graph.workflow import workflow_app

def main():
    # 模拟环境准备：入库测试数据
    rag_manager = RAGManager()
    
    # 假设你有一些 .txt 文件在 local_docs 目录下
    # 如果没文件夹就先跳过, 或手动建立个知识切片
    # rag_manager.ingest_directory("./knowledge_docs")

    # 执行测试 1: 智享商店相关 (走向 RAG)
    print("\n--- 测试场景 1: 智享商店知识检索 ---")
    inputs = {"question": "智享商店怎么开通会员？"}
    for output in workflow_app.stream(inputs):
        for key, value in output.items():
            print(f"节点 '{key}' 产生输出: {value}")

    # 执行测试 2: 通用问题 (走向 General LLM)
    print("\n--- 测试场景 2: 通用闲聊 ---")
    inputs_2 = {"question": "今天天气怎么样？"}
    for output in workflow_app.stream(inputs_2):
        for key, value in output.items():
            print(f"节点 '{key}' 产生输出: {value}")

if __name__ == "__main__":
    main()
