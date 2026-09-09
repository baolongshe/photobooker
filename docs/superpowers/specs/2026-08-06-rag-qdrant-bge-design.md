# 启用 RAG（Qdrant + bge-large-zh-v1.5，无 Elasticsearch）

日期：2026-08-06
状态：已批准（用户直接批准，不再询问）

## 目标

启用 `rag.enabled=true`，不使用 Elasticsearch，嵌入模型切换为 Ollama `quentinz/bge-large-zh-v1.5`（1024 维），向量存储用 Qdrant（本机 6333 端口已启动）。从 photo 库索引：摄影师作品描述（portfolio.description/title/tags）、个人介绍（photographer.intro）、经纬度（photographer.latitude/longitude）等字段。

最终回答使用 ModelScope DeepSeek（现有 spring.ai.openai 配置），查询改写/重排使用 Ollama 本地聊天模型。

## 架构

```
用户问题 → QueryRewrite(Ollama qwen2:1.5b) → 多路召回 → Rerank(Ollama，弱模型自动跳过)
                                              ├─ 向量检索: Qdrant (bge-large-zh 1024维)
                                              └─ 关键词检索: ES → 禁用时跳过
→ 拼上下文 → 最终回答: ModelScope DeepSeek (SpringAiChatService)
索引: MySQL (photographer/portfolio/package) → Ollama 嵌入 → Qdrant 1024维
```

## 环境事实（已实测）

- Ollama 运行中，`quentinz/bge-large-zh-v1.5` 已装，输出 1024 维 ✓
- Qdrant 运行中，`photobooker_knowledge` collection 存在但为 768 维（旧 nomic 索引），需重建
- ES 未运行且不需要；`spring.elasticsearch.uris` 是 ES 自动配置激活条件，删除后 ES 完全退出
- 数据：7 摄影师（均有经纬度字段）、52 套餐、12 作品集
- Ollama 本地聊天模型仅有 `qwen2:1.5b`（改写/重排用；重排对弱模型自动跳过）

## 改动清单

### 1. application.yml
- `rag.enabled: true`
- `rag.ollama.embedding-model: quentinz/bge-large-zh-v1.5`
- `rag.ollama.chat-model: qwen2:1.5b`
- 新增 `rag.qdrant.vector-size: 1024`
- 删除 `spring.elasticsearch.uris`

### 2. RagProperties.java
- `QdrantProperties` 新增 `vectorSize`（默认 1024）

### 3. QdrantVectorService.java
- 向量维度硬编码 768 → 读配置
- `ensureCollectionExists`：已存在 collection 但维度不匹配时删除重建

### 4. KnowledgeIndexService.java
- 摄影师文本追加经纬度：`坐标：纬度X，经度Y`
- metadata 扩展：摄影师 → latitude/longitude/location/style/name；作品集 → photographerName/category/tags/title
- ES 写入按 `rag.elasticsearch.enabled` 条件跳过

### 5. ElasticsearchService.java
- `available = ragProperties.getElasticsearch().isEnabled() && ops != null`

### 6. MultiPathRecallService.java
- ES 禁用时不提交关键词检索任务

### 7. RagPipelineService.java + AdminRagController.testQuery
- 最终回答由 OllamaClient 改为 SpringAiChatService（ModelScope DeepSeek），流式接口不变
- 查询改写/重排保持 Ollama

## 测试方案

1. `mvn compile` 编译通过
2. 启动服务，admin 登录拿 token
3. `POST /photo/admin/rag/index` 全量重建
4. 验证 Qdrant：collection 1024 维，约 71 点
5. `POST /ai/rag-chat-debug` 提问验证召回（婚纱摄影师、经纬度、语义检索）
6. 验证 `/ai/rag-chat` 流式回答走 DeepSeek
