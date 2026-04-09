# 智享商店 Agent & RAG 接口文档

## 1. 后台管理配置 (Admin)

用于在后台系统中动态管理 RAG 的全局运行参数，支持热更新。

### 1.1 获取系统配置
- **接口地址**: `/admin/rag-config`
- **请求方法**: `GET`
- **返回结果**:
  ```json
  {
    "embedding_model": "text-embedding-3-small",
    "chunk_size": 500,
    "chunk_overlap": 50,
    "chunk_mode": "recursive",
    "search_type": "hybrid",
    "weight_semantic": 0.7,
    "use_rerank": true,
    "rerank_model": "gpt-4o-mini"
  }
  ```

### 1.2 更新系统配置
- **接口地址**: `/admin/rag-config/update`
- **请求方法**: `POST`
- **请求参数**: `JSON Body` (支持局部更新)
  - 示例: `{"weight_semantic": 0.5, "use_rerank": false}`
- **功能**: 更新 `rag_config.json` 并实时触发 `RAGManager` 的配置重载。

---

## 2. RAG 知识入库管理

### 2.1 预览文档切分
- **接口地址**: `/rag/preview`
- **请求方法**: `POST`
- **内容类型**: `multipart/form-data`
- **请求参数**:
  | 参数名 | 类型 | 必选 | 说明 |
  | :--- | :--- | :--- | :--- |
  | file | File | 是 | 上传的知识文档 (.txt) |
  | mode | String | 否 | 切分模式 (默认取后台配置) |
  | chunk_size | Int | 否 | 切分块大小 (默认取后台配置) |
  | chunk_overlap | Int | 否 | 重叠大小 (默认取后台配置) |

- **返回结果**: 返回文件 ID 及前 3 条切分后的文本预览。

### 2.2 确认入库向量化
- **接口地址**: `/rag/commit`
- **请求方法**: `POST`
- **请求参数**: `filename` (预览接口返回的文件名)
- **功能**: 此时才执行真正的 `Embeddings` 向量化并持久化到本地 `FAISS` 索引。

---

## 3. Agent 工作流接口 (LangGraph)

### 3.1 智能问答与检索
- **检索逻辑**:
  1. **混合检索 (Hybrid)**: 同时查询向量库与关键词索引。
  2. **混合权重**: 由后台配置 `weight_semantic` 决定语义检索占比。
  3. **重排序 (Rerank)**: 若开启，则由 LLM 对初筛结果进行语义压缩。
- **动态生效**: 一旦后台通过 `/admin/rag-config/update` 修改，生成环节将立即按新逻辑执行。
