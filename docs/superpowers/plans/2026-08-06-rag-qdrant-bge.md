# RAG 启用（Qdrant 1024维 + bge-large-zh-v1.5，无 ES）实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 启用 RAG：嵌入模型换 Ollama `quentinz/bge-large-zh-v1.5`（1024维），向量存 Qdrant，禁用 ES，索引摄影师作品描述/个人介绍/经纬度，最终回答走 ModelScope DeepSeek，并重建索引验证。

**Architecture:** 沿用现有 RAG 管线（QueryRewrite → 多路召回 → Rerank → 生成）。仅切换：嵌入维度 768→1024（配置化并重建 collection）、ES 路径条件禁用、最终回答从 OllamaClient 改为 SpringAiChatService（ModelScope DeepSeek）。

**Tech Stack:** Spring Boot 3.2.5, Spring AI 1.0.0-M6, Qdrant REST API, Ollama, MyBatis-Plus, Redis

## Global Constraints

- 项目路径：`D:\JavaWork\PhotoBooker\PhotoBooker`（git 仓库，分支 new-branch）
- 嵌入模型：`quentinz/bge-large-zh-v1.5`，输出 1024 维（已实测）
- `rag.elasticsearch.enabled` 保持 false；删除 `spring.elasticsearch.uris`（ES 自动配置激活条件）
- 聊天模型（改写/重排，Ollama）：`qwen2:1.5b`（本地已有；重排对弱模型自动跳过，勿改 RerankService）
- 最终回答：SpringAiChatService（ModelScope DeepSeek），不直接调 Ollama chat
- 数据源：photo 库 photographer / portfolio / photographer_package 表，只读
- 数据库：192.168.100.129:3306，root/1234；Qdrant: localhost:6333；Ollama: localhost:11434

---

### Task 1: 配置层（application.yml + RagProperties）

**Files:**
- Modify: `src/main/resources/application.yml:137-157`
- Modify: `src/main/java/com/xhxi/photobooker/config/RagProperties.java:26-31`

**Interfaces:**
- Produces: `rag.qdrant.vector-size=1024` 配置键；`RagProperties.QdrantProperties.vectorSize`（int，默认 1024）

- [ ] **Step 1: 修改 application.yml 的 rag 段与 spring.elasticsearch**

将：
```yaml
rag:
  enabled: false
  ollama:
    base-url: http://localhost:11434
    chat-model: deepseek-ai/DeepSeek-V4-Flash-0731
    embedding-model: nomic-embed-text
  qdrant:
    host: localhost
    port: 6333
    collection-name: photobooker_knowledge
  elasticsearch:
    enabled: false
    host: 192.168.100.129
    port: 9200
    index-name: photobooker_knowledge
```
改为：
```yaml
rag:
  enabled: true
  ollama:
    base-url: http://localhost:11434
    chat-model: qwen2:1.5b
    embedding-model: quentinz/bge-large-zh-v1.5
  qdrant:
    host: localhost
    port: 6333
    collection-name: photobooker_knowledge
    vector-size: 1024
  elasticsearch:
    enabled: false
    host: 192.168.100.129
    port: 9200
    index-name: photobooker_knowledge
```

删除文件末尾的：
```yaml
spring.elasticsearch:
  uris: http://${RAG_ES_HOST:192.168.100.129}:${RAG_ES_PORT:9200}
```

- [ ] **Step 2: RagProperties.QdrantProperties 增加 vectorSize**

```java
public static class QdrantProperties {
    private String host = "localhost";
    private int port = 6333;
    private String collectionName = "photobooker_knowledge";
    private int vectorSize = 1024;
}
```

- [ ] **Step 3: 提交**

```bash
git add src/main/resources/application.yml src/main/java/com/xhxi/photobooker/config/RagProperties.java
git commit -m "config: enable RAG with qdrant 1024-dim and bge-large-zh-v1.5, disable ES"
```

---

### Task 2: QdrantVectorService 维度配置化 + collection 重建

**Files:**
- Modify: `src/main/java/com/xhxi/photobooker/service/rag/QdrantVectorService.java`

**Interfaces:**
- Consumes: `RagProperties.getQdrant().getVectorSize()`（Task 1 产出）
- Produces: 构造后 `init()` 保证 collection 维度 == 配置维度（不匹配则删除重建）；`search/upsert/delete/getCollectionInfo/getAllDocuments` 签名不变

- [ ] **Step 1: 构造器读取配置维度**

把 `private final int vectorSize;` 的赋值从硬编码 `768` 改为：
```java
this.vectorSize = qdrantProps.getVectorSize();
```
（移除注释 `// nomic-embed-text output dimension`）

- [ ] **Step 2: ensureCollectionExists 增加维度校验**

将 `ensureCollectionExists()` 替换为：
```java
private void ensureCollectionExists() {
    try {
        Map<String, Object> response = webClient.get()
                .uri("/collections/" + collectionName)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        int existingSize = extractVectorSize(response);
        if (existingSize != vectorSize) {
            log.info("Qdrant collection '{}' has size {}, expected {}. Recreating...",
                    collectionName, existingSize, vectorSize);
            deleteCollection();
            createCollection();
        } else {
            log.info("Qdrant collection '{}' exists with size {}", collectionName, existingSize);
        }
    } catch (Exception e) {
        log.info("Qdrant collection '{}' not found, creating...", collectionName);
        createCollection();
    }
}

private int extractVectorSize(Map<String, Object> response) {
    if (response == null) return -1;
    Object resultObj = response.get("result");
    if (!(resultObj instanceof Map<?, ?> result)) return -1;
    Object configObj = result.get("config");
    if (!(configObj instanceof Map<?, ?> config)) return -1;
    Object paramsObj = config.get("params");
    if (!(paramsObj instanceof Map<?, ?> params)) return -1;
    Object vectorsObj = params.get("vectors");
    if (vectorsObj instanceof Map<?, ?> vectors) {
        Object sizeObj = vectors.get("size");
        return sizeObj instanceof Number n ? n.intValue() : -1;
    }
    return -1;
}

private void deleteCollection() {
    webClient.delete()
            .uri("/collections/" + collectionName)
            .retrieve()
            .bodyToMono(Map.class)
            .block();
    log.info("Qdrant collection '{}' deleted", collectionName);
}
```

注意：`createCollection()` 保持原有实现（用 `vectorSize` 字段）。若 GET 失败（连接异常）走 catch 分支创建，保持原行为。WebClient 有 `.delete()` 方法，无需额外依赖。

- [ ] **Step 3: 编译验证**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add src/main/java/com/xhxi/photobooker/service/rag/QdrantVectorService.java
git commit -m "feat: qdrant vector size from config, auto-recreate mismatched collection"
```

---

### Task 3: KnowledgeIndexService 内容增强（经纬度 + metadata + ES 条件写入）

**Files:**
- Modify: `src/main/java/com/xhxi/photobooker/service/rag/KnowledgeIndexService.java`

**Interfaces:**
- Consumes: `RagProperties.getElasticsearch().isEnabled()`（判断 ES 是否写入）
- Produces: 索引内容含经纬度；metadata 含 latitude/longitude 等；`rebuildFullIndex/incrementalIndex/deleteDocument/getDocumentCount` 签名不变

- [ ] **Step 1: 注入 RagProperties**

构造器参数列表末尾追加 `RagProperties ragProperties`，并赋给新字段 `private final RagProperties ragProperties;`

- [ ] **Step 2: buildPhotographerText 追加经纬度**

在 `buildPhotographerText` 中，`sb.append("。").append(p.getIntro());` 之后追加：
```java
if (p.getLatitude() != null && p.getLongitude() != null) {
    sb.append("。坐标：纬度").append(p.getLatitude()).append("，经度").append(p.getLongitude());
}
```

- [ ] **Step 3: 摄影师 metadata 扩展**

`rebuildFullIndex` 和 `incrementalIndex` 中的摄影师 `buildMetadata` 调用改为：
```java
Map<String, Object> metadata = buildMetadata("photographer", p.getId(), linkedMapOf(
        "name", nullSafe(p.getName()),
        "style", p.getStyle(),
        "location", p.getLocation(),
        "latitude", p.getLatitude(),
        "longitude", p.getLongitude()));
```

- [ ] **Step 4: 作品集 metadata 扩展**

`rebuildFullIndex` 和 `incrementalIndex` 中的作品集调用改为：
```java
Map<String, Object> metadata = buildMetadata("portfolio", pf.getId(), linkedMapOf(
        "title", nullSafe(pf.getTitle()),
        "photographerId", pf.getPhotographerId(),
        "photographerName", photographerMap.get(pf.getPhotographerId()) != null
                ? photographerMap.get(pf.getPhotographerId()).getName() : null,
        "category", pf.getCategory(),
        "tags", pf.getTags()));
```

- [ ] **Step 5: ES 写入条件化**

`indexSingleDocument` 中 ES 部分改为：
```java
// Write to Elasticsearch only if enabled (default off)
if (ragProperties.getElasticsearch().isEnabled()) {
    KnowledgeDocument knowledgeDoc = KnowledgeDocument.builder()
            .id(docId)
            .content(text)
            .source((String) metadata.get("source"))
            .sourceId((Long) metadata.get("sourceId"))
            .metadata(metadata)
            .createdAt(LocalDateTime.now())
            .build();
    elasticsearchService.indexDocument(knowledgeDoc);
}
```

- [ ] **Step 6: 提交**

```bash
git add src/main/java/com/xhxi/photobooker/service/rag/KnowledgeIndexService.java
git commit -m "feat: index lat/lng and richer metadata; ES write gated by config"
```

---

### Task 4: ES 禁用路径（ElasticsearchService + MultiPathRecallService）

**Files:**
- Modify: `src/main/java/com/xhxi/photobooker/service/rag/ElasticsearchService.java:32-40`
- Modify: `src/main/java/com/xhxi/photobooker/service/rag/MultiPathRecallService.java:67-147`

**Interfaces:**
- Produces: `ElasticsearchService.available` = enabled && ops != null；`MultiPathRecallService.recall` 在 ES 禁用时只做向量检索

- [ ] **Step 1: ElasticsearchService.available 双条件**

```java
this.available = ragProperties.getElasticsearch().isEnabled() && elasticsearchOperations != null;
```

- [ ] **Step 2: MultiPathRecallService 跳过 ES 检索**

在 `recall()` 循环前定义 `boolean esEnabled = ragProperties.getElasticsearch().isEnabled();`，循环内改为：
```java
List<CompletableFuture<List<SearchResult>>> futures = new ArrayList<>();

for (String query : queries) {
    CompletableFuture<List<SearchResult>> vectorFuture = CompletableFuture.supplyAsync(() -> {
        try {
            float[] embedding = ollamaClient.generateEmbedding(query);
            return qdrantVectorService.search(embedding, topK, scoreThreshold);
        } catch (Exception e) {
            log.error("Vector search failed for query [{}]: {}", query, e.getMessage());
            return Collections.emptyList();
        }
    }, recallExecutor);
    futures.add(vectorFuture);

    if (esEnabled) {
        CompletableFuture<List<SearchResult>> keywordFuture = CompletableFuture.supplyAsync(() -> {
            try {
                List<ElasticsearchService.SearchResult> esResults =
                        elasticsearchService.search(query, topK);
                return convertEsResults(esResults);
            } catch (Exception e) {
                log.error("Keyword search failed for query [{}]: {}", query, e.getMessage());
                return Collections.emptyList();
            }
        }, recallExecutor);
        futures.add(keywordFuture);
    }
}
```

收集段改为（每查询从 2 项变为「1 项（纯向量）或 2 项（带 ES）」）：
```java
// Collect all vector and keyword results per query, then merge
List<SearchResult> allMerged = new ArrayList<>();

int i = 0;
for (String query : queries) {
    try {
        List<SearchResult> vectorResults = futures.get(i).getNow(Collections.emptyList());
        List<SearchResult> keywordResults = esEnabled
                ? futures.get(i + 1).getNow(Collections.emptyList())
                : Collections.emptyList();
        List<SearchResult> merged = resultMerger.merge(vectorResults, keywordResults, topK);
        allMerged.addAll(merged);
    } catch (Exception e) {
        log.error("Failed to collect results for query [{}]: {}", query, e.getMessage());
    }
    i += esEnabled ? 2 : 1;
}
```

- [ ] **Step 3: 编译验证**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add src/main/java/com/xhxi/photobooker/service/rag/ElasticsearchService.java src/main/java/com/xhxi/photobooker/service/rag/MultiPathRecallService.java
git commit -m "feat: fully skip ES when rag.elasticsearch.enabled=false"
```

---

### Task 5: 最终回答切换 SpringAiChatService（ModelScope DeepSeek）

**Files:**
- Modify: `src/main/java/com/xhxi/photobooker/service/rag/RagPipelineService.java`
- Modify: `src/main/java/com/xhxi/photobooker/controller/AdminRagController.java`

**Interfaces:**
- Consumes: `SpringAiChatService.streamChat(String systemPrompt, String userMessage)`（已有方法，返回 `Flux<String>`）
- Produces: RAG 最终回答来自 ModelScope DeepSeek；改写/重排仍走 Ollama

- [ ] **Step 1: RagPipelineService 注入 SpringAiChatService**

```java
private final SpringAiChatService springAiChatService;
```
构造器追加参数 `SpringAiChatService springAiChatService` 并赋值。

- [ ] **Step 2: ragQuery 流式回答切换**

`ragQuery` 末尾：
```java
return ollamaClient.chatCompletionStream(systemPrompt, question)
```
改为：
```java
return springAiChatService.streamChat(systemPrompt, question)
```

- [ ] **Step 3: ragQueryDebug 非流式回答切换**

`ragQueryDebug` 末尾：
```java
String finalAnswer = ollamaClient.chatCompletion(systemPrompt, question);
```
改为：
```java
String finalAnswer = springAiChatService.streamChat(systemPrompt, question)
        .blockLast(Duration.ofMinutes(2));
```
（import 补 `java.time.Duration`；若 `chatCompletion` 不再使用且 `ollamaClient` 字段仅用于此，保留字段不删——OllamaClient 仍被改写/重排服务使用，但 RagPipelineService 中若无其它用途则删除该字段和 import）

- [ ] **Step 4: AdminRagController.testQuery 回答切换**

注入 `@Autowired private SpringAiChatService springAiChatService;`，将：
```java
String rawAnswer = ollamaClient.chatCompletion(systemPrompt, question);
```
改为：
```java
String rawAnswer = springAiChatService.streamChat(systemPrompt, question)
        .blockLast(java.time.Duration.ofMinutes(2));
```

- [ ] **Step 5: 编译验证**

Run: `mvn compile -q`
Expected: BUILD SUCCESS（若 OllamaClient 在 RagPipelineService 无剩余引用，需同步删字段与 import）

- [ ] **Step 6: 提交**

```bash
git add src/main/java/com/xhxi/photobooker/service/rag/RagPipelineService.java src/main/java/com/xhxi/photobooker/controller/AdminRagController.java
git commit -m "feat: RAG final answer via ModelScope DeepSeek (SpringAiChatService)"
```

---

### Task 6: 重建索引 + 端到端测试

**Files:**
- 无代码改动（仅运行与验证）

**前置条件：** 应用已编译（Task 1-5 完成）。启动应用前确认：Qdrant `localhost:6333` 运行中、Ollama `localhost:11434` 运行中、MySQL `192.168.100.129:3306` 可达、Redis `192.168.100.129:6379` 可达。

- [ ] **Step 1: 启动应用**

Run（后台）: `mvn spring-boot:run`（工作目录 `PhotoBooker\PhotoBooker`）
Expected: 日志出现 `QdrantVectorService initialized` 与 `Qdrant collection 'photobooker_knowledge' exists with size 768`（或 recreating 日志）

- [ ] **Step 2: admin 登录拿 token**

Run:
```powershell
$body = @{ username = '<admin账号>'; password = '<密码>' } | ConvertTo-Json
Invoke-RestMethod -Uri "http://localhost:8099/photo/admin/login" -Method Post -Body $body -ContentType "application/json"
```
（admin 账号密码从 admin 表查：`SELECT username, password FROM photo.admin`；登录接口路径以 AdminController 实际为准）
Expected: 返回 JSON 含 token

- [ ] **Step 3: 触发全量重建**

Run:
```powershell
Invoke-RestMethod -Uri "http://localhost:8099/photo/admin/rag/index" -Method Post -Headers @{ ADMINTOKEN = "<token>" }
```
Expected: `{"status":"started",...}`；旧 768 维 collection 被自动删除重建为 1024 维

- [ ] **Step 4: 验证 Qdrant collection**

Run: `Invoke-RestMethod -Uri "http://localhost:6333/collections/photobooker_knowledge"`
Expected: `"size": 1024`；`points_count` ≈ 71（7 摄影师 + 52 套餐 + 12 作品）

- [ ] **Step 5: 调试查询验证召回与经纬度**

Run:
```powershell
$body = @{ question = "推荐擅长婚纱摄影的摄影师" } | ConvertTo-Json
Invoke-RestMethod -Uri "http://localhost:8099/photo/admin/rag/test-query" -Method Post -Headers @{ ADMINTOKEN = "<token>" } -Body $body -ContentType "application/json"
```
Expected: recallResults 非空，content 含摄影师信息；再问「坐标」「经纬度」验证内容含 `坐标：纬度...，经度...`

- [ ] **Step 6: 验证流式回答（DeepSeek）**

Run:
```powershell
$body = @{ question = "有哪些摄影师擅长婚纱摄影" } | ConvertTo-Json
Invoke-RestMethod -Uri "http://localhost:8099/ai/rag-chat-debug" -Method Post -Headers @{ ADMINTOKEN = "<token>" } -Body $body -ContentType "application/json"
```
Expected: finalAnswer 非空且为中文回答；应用日志确认走 Spring AI（ModelScope）

- [ ] **Step 7: 验证 ES 完全禁用**

Expected: 启动日志与运行日志中无任何 `Elasticsearch` 连接尝试/报错；`/photo/admin/rag/status` 中 elasticsearch 状态为 disabled

- [ ] **Step 8: 提交**

无代码改动则跳过；若测试中发现小修复，单独提交。
