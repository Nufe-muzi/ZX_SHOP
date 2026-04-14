from pathlib import Path
import sys
import uvicorn
import argparse

# Allow running this file directly: `python agent_src/main.py`
if __package__ is None or __package__ == "":
    project_root = Path(__file__).resolve().parent.parent
    project_root_str = str(project_root)
    if project_root_str not in sys.path:
        sys.path.insert(0, project_root_str)

from agent_src.rag.api import app
from agent_src.rag.rag_store import RAGManager
from agent_src.graph.workflow import workflow_app


def run_api():
    """启动 API 服务"""
    print("=" * 50)
    print("RAG 系统 API 服务启动中...")
    print("=" * 50)
    print(f"API 服务: http://127.0.0.1:8000")
    print(f"API 文档: http://127.0.0.1:8000/docs")
    print(f"测试页面: test/html/rag_test.html")
    print("=" * 50)

    uvicorn.run(
        "agent_src.rag.api:app",
        host="127.0.0.1",
        port=8000,
        reload=True
    )


def run_test():
    """运行工作流测试"""
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


def main():
    parser = argparse.ArgumentParser(description="RAG 系统启动器")
    parser.add_argument(
        "command",
        choices=["api", "test", "all"],
        default="all",
        nargs="?",
        help="api: 仅启动API服务 | test: 仅运行测试 | all: 启动API服务(默认)"
    )
    args = parser.parse_args()

    if args.command == "api":
        run_api()
    elif args.command == "test":
        run_test()
    else:
        run_api()


if __name__ == "__main__":
    main()
