from typing import TypedDict, List, Annotated, Literal
from langchain_openai import ChatOpenAI
from langchain_core.messages import BaseMessage, HumanMessage
from agent_src.rag.rag_store import RAGManager
from agent_src.setting.aliyun.config import aliyun_config

# 定义工作流状态
class GraphState(TypedDict):
    question: str
    generation: str
    documents: List[str]

# 初始化模型和RAG管理器
llm = ChatOpenAI(**aliyun_config.get_llm_kwargs(), temperature=0)
rag_manager = RAGManager()

def retrieve_node(state):
    """
    检索相关的智享商店知识
    """
    print("--- 步骤: RETRIEVE ---")
    question = state["question"]
    retriever = rag_manager.get_advanced_retriever()
    documents = retriever.invoke(question)
    doc_texts = [d.page_content for d in documents]
    return {"documents": doc_texts}

def generate_node(state):
    """
    基于检索结果生成回答
    """
    print("--- 步骤: GENERATE ---")
    question = state["question"]
    docs = state["documents"]
    
    context = "\n\n".join(docs)
    prompt = f"你是智享商店的助手。基于以下上下文回答问题：\n\n{context}\n\n问题：{question}"
    
    response = llm.invoke([HumanMessage(content=prompt)])
    return {"generation": response.content}

def general_llm_node(state):
    """
    通用 LLM 处理逻辑 (非智享商店相关)
    """
    print("--- 步骤: GENERAL LLM ---")
    question = state["question"]
    response = llm.invoke([HumanMessage(content=question)])
    return {"generation": response.content}

def router_node(state):
    """
    路由逻辑：决定走向 RAG 还是 通用回话
    """
    print("--- 步骤: ROUTE ---")
    question = state["question"]
    
    # 构建意图识别 prompt
    router_prompt = f"""
    判断以下用户输入是否涉及我们的产品'智享商店'(Shop2/Smart Shop)的相关内容。
    如果是, 返回 'rag'；
    如果不是 (通用问题、问候或其他), 返回 'general'。
    
    用户输入: {question}
    
    仅返回 'rag' 或 'general' 字符串。
    """
    
    response = llm.invoke([HumanMessage(content=router_prompt)])
    decision = response.content.strip().lower()
    
    if "rag" in decision:
        return "retrieve"
    else:
        return "general"
