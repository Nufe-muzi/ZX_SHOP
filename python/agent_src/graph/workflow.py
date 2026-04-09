from langgraph.graph import StateGraph, END
from agent_src.graph.nodes import (
    GraphState, 
    retrieve_node, 
    generate_node, 
    general_llm_node, 
    router_node
)

def build_workflow():
    workflow = StateGraph(GraphState)

    # 添加节点
    workflow.add_node("retrieve", retrieve_node)
    workflow.add_node("generate", generate_node)
    workflow.add_node("general_llm", general_llm_node)

    # 设置路由逻辑
    workflow.set_conditional_entry_point(
        router_node,
        {
            "retrieve": "retrieve",
            "general": "general_llm"
        }
    )

    # 设置 RAG 线流程：检索 -> 生成
    workflow.add_edge("retrieve", "generate")
    
    # 设置所有生成流向结束
    workflow.add_edge("generate", END)
    workflow.add_edge("general_llm", END)

    return workflow.compile()

# 导出工作流
workflow_app = build_workflow()
