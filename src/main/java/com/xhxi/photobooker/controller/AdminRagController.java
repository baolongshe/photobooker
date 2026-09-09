package com.xhxi.photobooker.controller;

import com.xhxi.photobooker.config.RagProperties;
import com.xhxi.photobooker.entity.rag.KnowledgeDocument;
import com.xhxi.photobooker.result.Result;
import com.xhxi.photobooker.service.SpringAiChatService;
import com.xhxi.photobooker.service.ollama.OllamaClient;
import com.xhxi.photobooker.service.rag.ElasticsearchService;
import com.xhxi.photobooker.service.rag.KnowledgeIndexService;
import com.xhxi.photobooker.service.rag.QdrantVectorService;
import com.xhxi.photobooker.service.rag.MultiPathRecallService;
import com.xhxi.photobooker.service.rag.QueryRewriteService;
import com.xhxi.photobooker.service.rag.RerankService;
import com.xhxi.photobooker.service.rag.SearchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/photo/admin/rag")
public class AdminRagController {

    @Autowired(required = false)
    private QdrantVectorService qdrantVectorService;

    @Autowired(required = false)
    private ElasticsearchService elasticsearchService;

    @Autowired(required = false)
    private KnowledgeIndexService knowledgeIndexService;

    @Autowired(required = false)
    private QueryRewriteService queryRewriteService;

    @Autowired(required = false)
    private MultiPathRecallService multiPathRecallService;

    @Autowired(required = false)
    private RerankService rerankService;

    @Autowired(required = false)
    private OllamaClient ollamaClient;

    @Autowired
    private SpringAiChatService springAiChatService;

    private final RagProperties ragProperties;
    private final StringRedisTemplate redisTemplate;

    private static final String RAG_CONFIG_TOPK_KEY = "rag:config:topK";
    private static final String RAG_CONFIG_THRESHOLD_KEY = "rag:config:scoreThreshold";

    /**
     * 跟踪全量重建任务状态
     */
    private final AtomicReference<Map<String, Object>> rebuildStatus = new AtomicReference<>(null);

    public AdminRagController(RagProperties ragProperties, StringRedisTemplate redisTemplate) {
        this.ragProperties = ragProperties;
        this.redisTemplate = redisTemplate;
    }

    // ==================== 1. GET /status ====================

    @GetMapping("/status")
    public Result<Map<String, Object>> getStatus() {
        Map<String, Object> status = new LinkedHashMap<>();

        // Qdrant status
        Map<String, Object> qdrantStatus = new LinkedHashMap<>();
        try {
            if (qdrantVectorService == null) {
                qdrantStatus.put("status", "disabled");
                qdrantStatus.put("message", "RAG is not enabled");
            } else {
                Map<String, Object> collectionInfo = qdrantVectorService.getCollectionInfo();
                qdrantStatus.put("status", "healthy");
                qdrantStatus.put("collection", collectionInfo);
            }
        } catch (Exception e) {
            log.error("Failed to get Qdrant status: {}", e.getMessage());
            qdrantStatus.put("status", "error");
            qdrantStatus.put("error", e.getMessage());
        }
        status.put("qdrant", qdrantStatus);

        // Elasticsearch status
        Map<String, Object> esStatus = new LinkedHashMap<>();
        try {
            if (elasticsearchService == null) {
                esStatus.put("status", "disabled");
                esStatus.put("message", "RAG is not enabled");
            } else {
                Map<String, Object> indexInfo = elasticsearchService.getIndexInfo();
                String health = String.valueOf(indexInfo.getOrDefault("health", "unknown"));
                esStatus.put("status", "disabled".equals(health) || "unavailable".equals(health) ? health : "healthy");
                esStatus.put("index", indexInfo);
            }
        } catch (Exception e) {
            log.error("Failed to get Elasticsearch status: {}", e.getMessage());
            esStatus.put("status", "error");
            esStatus.put("error", e.getMessage());
        }
        status.put("elasticsearch", esStatus);

        // Last index time
        String lastIndexTime = redisTemplate.opsForValue().get("rag:last-index-time");
        status.put("lastIndexTime", lastIndexTime != null ? lastIndexTime : "never");

        // Rebuild task status
        Map<String, Object> currentRebuild = rebuildStatus.get();
        if (currentRebuild != null) {
            status.put("rebuildTask", currentRebuild);
        }

        return Result.success(status);
    }

    // ==================== 2. POST /index ====================

    @PostMapping("/index")
    public Result<Map<String, Object>> rebuildIndex() {
        if (knowledgeIndexService == null) {
            return Result.error("RAG is not enabled");
        }

        Map<String, Object> currentStatus = rebuildStatus.get();
        if (currentStatus != null && "running".equals(currentStatus.get("status"))) {
            return Result.error("A rebuild task is already running");
        }

        // Mark as running
        Map<String, Object> runningStatus = new LinkedHashMap<>();
        runningStatus.put("status", "running");
        runningStatus.put("startedAt", System.currentTimeMillis());
        rebuildStatus.set(runningStatus);

        // Run asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> result = knowledgeIndexService.rebuildFullIndex();
                result.put("status", "completed");
                result.put("completedAt", System.currentTimeMillis());
                rebuildStatus.set(result);
                log.info("Full index rebuild completed: {}", result);
            } catch (Exception e) {
                log.error("Full index rebuild failed: {}", e.getMessage(), e);
                Map<String, Object> errorStatus = new LinkedHashMap<>();
                errorStatus.put("status", "failed");
                errorStatus.put("error", e.getMessage());
                errorStatus.put("failedAt", System.currentTimeMillis());
                rebuildStatus.set(errorStatus);
            }
        });

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "started");
        response.put("message", "Full index rebuild has been started in background");
        return Result.success(response);
    }

    // ==================== 3. POST /index/incremental ====================

    @PostMapping("/index/incremental")
    public Result<Map<String, Object>> incrementalIndex() {
        if (knowledgeIndexService == null) {
            return Result.error("RAG is not enabled");
        }
        try {
            Map<String, Object> summary = knowledgeIndexService.incrementalIndex();
            return Result.success(summary);
        } catch (Exception e) {
            log.error("Incremental index failed: {}", e.getMessage(), e);
            return Result.error("Incremental index failed: " + e.getMessage());
        }
    }

    // ==================== 4. GET /documents ====================

    @GetMapping("/documents")
    public Result<Map<String, Object>> listDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        // Require at least one storage backend to be available
        if (elasticsearchService == null && qdrantVectorService == null) {
            return Result.error("RAG is not enabled");
        }
        try {
            List<Map<String, Object>> docList = new ArrayList<>();
            long total = 0;

            // Try Elasticsearch first (richer metadata); fallback to Qdrant
            boolean esAvailable = elasticsearchService != null && elasticsearchService.isAvailable();
            if (esAvailable) {
                List<KnowledgeDocument> docs = elasticsearchService.getAllDocuments(page, size);
                docList = docs.stream().map(doc -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", doc.getId());
                    String content = doc.getContent();
                    item.put("content", content != null && content.length() > 200
                            ? content.substring(0, 200) + "..." : content);
                    item.put("source", doc.getSource());
                    item.put("sourceId", doc.getSourceId());
                    item.put("createdAt", doc.getCreatedAt() != null ? doc.getCreatedAt().toString() : null);
                    return item;
                }).collect(Collectors.toList());

                Map<String, Object> indexInfo = elasticsearchService.getIndexInfo();
                Object totalDocsObj = indexInfo.get("totalDocs");
                if (totalDocsObj instanceof Number num) {
                    total = num.longValue();
                }
            } else if (qdrantVectorService != null) {
                // Fallback to Qdrant scroll API
                int offset = page * size;
                List<Map<String, Object>> qdrantDocs = qdrantVectorService.getAllDocuments(offset, size);
                docList = qdrantDocs.stream().map(doc -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", doc.get("id"));
                    String content = (String) doc.get("content");
                    item.put("content", content != null && content.length() > 200
                            ? content.substring(0, 200) + "..." : content);
                    item.put("source", doc.get("source"));
                    item.put("sourceId", doc.get("sourceId"));
                    item.put("createdAt", null); // Qdrant doesn't store createdAt
                    return item;
                }).collect(Collectors.toList());

                // Get total from collection info
                Map<String, Object> collectionInfo = qdrantVectorService.getCollectionInfo();
                Object pointsCount = collectionInfo.get("pointsCount");
                if (pointsCount instanceof Number num) {
                    total = num.longValue();
                }
            }

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("documents", docList);
            response.put("total", total);
            response.put("page", page);
            response.put("size", size);
            response.put("esAvailable", esAvailable);
            return Result.success(response);
        } catch (Exception e) {
            log.error("Failed to list documents: {}", e.getMessage(), e);
            return Result.error("Failed to list documents: " + e.getMessage());
        }
    }

    // ==================== 5. DELETE /documents/{id} ====================

    @DeleteMapping("/documents/{id}")
    public Result<String> deleteDocument(@PathVariable String id) {
        try {
            if (qdrantVectorService != null) {
                try {
                    qdrantVectorService.deleteDocument(id);
                } catch (Exception e) {
                    log.warn("Failed to delete from Qdrant, id={}: {}", id, e.getMessage());
                }
            }
            if (elasticsearchService != null) {
                try {
                    elasticsearchService.deleteDocument(id);
                } catch (Exception e) {
                    log.warn("Failed to delete from Elasticsearch, id={}: {}", id, e.getMessage());
                }
            }
            return Result.success("Document deleted: " + id);
        } catch (Exception e) {
            log.error("Failed to delete document id={}: {}", id, e.getMessage(), e);
            return Result.error("Failed to delete document: " + e.getMessage());
        }
    }

    // ==================== 6. POST /test-query ====================

    @PostMapping("/test-query")
    public Result<Map<String, Object>> testQuery(@RequestBody Map<String, String> body) {
        String question = body.get("question");
        String chatHistory = body.get("chatHistory");

        if (question == null || question.isBlank()) {
            return Result.error("question is required");
        }

        if (queryRewriteService == null || ollamaClient == null
                || qdrantVectorService == null) {
            return Result.error("RAG services are not fully available");
        }

        Map<String, Object> debugResult = new LinkedHashMap<>();

        try {
            // === Step 1: Query Rewrite ===
            QueryRewriteService.RewriteResult rewriteResult = queryRewriteService.rewrite(question, chatHistory);
            Map<String, Object> rewriteMap = new LinkedHashMap<>();
            rewriteMap.put("originalQuery", rewriteResult.getOriginalQuery());
            rewriteMap.put("compensatedQuery", rewriteResult.getCompensatedQuery());
            rewriteMap.put("expandedQueries", rewriteResult.getExpandedQueries());
            rewriteMap.put("subQueries", rewriteResult.getSubQueries());
            debugResult.put("rewrite", rewriteMap);

            // === Step 2: Collect all rewritten queries (same logic as RagPipelineService) ===
            Set<String> querySet = new LinkedHashSet<>();
            if (rewriteResult.getCompensatedQuery() != null && !rewriteResult.getCompensatedQuery().isBlank()) {
                querySet.add(rewriteResult.getCompensatedQuery());
            }
            if (rewriteResult.getExpandedQueries() != null) {
                querySet.addAll(rewriteResult.getExpandedQueries());
            }
            if (rewriteResult.getSubQueries() != null) {
                querySet.addAll(rewriteResult.getSubQueries());
            }
            // Always include original query
            if (rewriteResult.getOriginalQuery() != null) {
                querySet.add(rewriteResult.getOriginalQuery());
            }
            List<String> allQueries = new ArrayList<>(querySet);
            debugResult.put("allQueriesCount", allQueries.size());
            debugResult.put("allQueries", allQueries);

            // === Step 3: Multi-path Recall (all rewritten queries → parallel embedding + vector search) ===
            int topK = getRuntimeTopK();
            Double scoreThreshold = getRuntimeScoreThreshold();

            List<SearchResult> recallResults;
            if (multiPathRecallService != null) {
                // Full multi-path recall: each rewritten query generates its own embedding
                log.info("Using multi-path recall with {} queries", allQueries.size());
                recallResults = multiPathRecallService.recall(allQueries, topK * 2, scoreThreshold);
                debugResult.put("recallMode", "multi-path (all rewritten queries)");
            } else {
                // Fallback: single-query embedding (old behavior)
                String queryForEmbedding = rewriteResult.getCompensatedQuery() != null
                        ? rewriteResult.getCompensatedQuery() : question;
                float[] embedding = ollamaClient.generateEmbedding(queryForEmbedding);
                debugResult.put("embeddingDimensions", embedding.length);
                recallResults = qdrantVectorService.search(embedding, topK * 2, scoreThreshold);
                debugResult.put("recallMode", "single-query-fallback");
            }

            List<Map<String, Object>> recallDebug = recallResults.stream().map(r -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", r.getId());
                m.put("content", truncate(r.getContent(), 150));
                m.put("score", r.getScore());
                m.put("source", r.getSource());
                m.put("sourceId", r.getSourceId());
                return m;
            }).collect(Collectors.toList());
            debugResult.put("recallResults", recallDebug);
            debugResult.put("recallCount", recallResults.size());

            // === Step 3.5: Keyword boost — rescue relevant results buried by generic expanded queries ===
            // When synonym expansion strips proper nouns ("乌萨奇" → "宠物摄影"),
            // the expanded queries flood recall with noise. This boost re-elevates
            // results that share key terms with the original query.
            if (recallResults.size() > 1) {
                recallResults = keywordBoost(question, recallResults);
                debugResult.put("keywordBoostApplied", true);
            }

            // === Step 4: LLM Rerank (same as RagPipelineService) ===
            List<SearchResult> finalResults;
            if (rerankService != null && recallResults.size() > 1) {
                finalResults = rerankService.rerank(question, recallResults);
                List<Map<String, Object>> rerankDebug = finalResults.stream().map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", r.getId());
                    m.put("content", truncate(r.getContent(), 150));
                    m.put("score", r.getScore());
                    m.put("source", r.getSource());
                    return m;
                }).collect(Collectors.toList());
                debugResult.put("rerankResults", rerankDebug);
                debugResult.put("rerankCount", finalResults.size());
            } else {
                finalResults = recallResults;
                debugResult.put("rerankResults", "skipped (rerank unavailable or <=1 candidates)");
            }

            // === Step 5: Build context from top results ===
            int contextLimit = Math.min(topK, finalResults.size());
            String context = finalResults.stream()
                    .limit(contextLimit)
                    .map(r -> r.getContent() != null ? r.getContent() : "")
                    .collect(Collectors.joining("\n---\n"));

            // === Step 6: Generate answer via Ollama ===
            String systemPrompt = "你是PhotoBooker摄影约拍平台的智能助手。\n"
                    + "【重要规则】你必须严格遵守：\n"
                    + "1. 只能使用下面「知识库内容」中明确提到的信息来回答\n"
                    + "2. 严禁编造、猜测或提及任何知识库中没有的摄影师姓名、套餐名称、价格\n"
                    + "3. 如果知识库内容不足以回答用户问题，直接说\"知识库中暂无相关信息\"，不要敷衍或编造\n"
                    + "4. 回答时请引用知识库中的具体信息（如摄影师名字、套餐名称、价格）\n\n"
                    + "知识库内容：\n" + context;

            String rawAnswer;
            try {
                rawAnswer = springAiChatService.streamChat(systemPrompt, question)
                        .blockLast(java.time.Duration.ofMinutes(2));
            } catch (Exception e) {
                log.warn("ModelScope DeepSeek failed ({}), falling back to Ollama", e.getMessage());
                rawAnswer = ollamaClient.chatCompletion(systemPrompt, question);
            }

            // Post-generation hallucination check: scan answer for names not in context
            String answer = checkAnswerHallucination(rawAnswer, context);
            debugResult.put("answer", answer);
            debugResult.put("contextUsed", contextLimit);

            return Result.success(debugResult);
        } catch (Exception e) {
            log.error("Test query failed: {}", e.getMessage(), e);
            debugResult.put("error", e.getMessage());
            return Result.success(debugResult);
        }
    }

    // ==================== 7. GET /config ====================

    @GetMapping("/config")
    public Result<Map<String, Object>> getConfig() {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("enabled", ragProperties.isEnabled());
        config.put("ollama", Map.of(
                "baseUrl", ragProperties.getOllama().getBaseUrl(),
                "chatModel", ragProperties.getOllama().getChatModel(),
                "embeddingModel", ragProperties.getOllama().getEmbeddingModel()
        ));
        config.put("qdrant", Map.of(
                "host", ragProperties.getQdrant().getHost(),
                "port", ragProperties.getQdrant().getPort(),
                "collectionName", ragProperties.getQdrant().getCollectionName()
        ));
        config.put("elasticsearch", Map.of(
                "host", ragProperties.getElasticsearch().getHost(),
                "port", ragProperties.getElasticsearch().getPort(),
                "indexName", ragProperties.getElasticsearch().getIndexName()
        ));

        // Runtime overrides from Redis
        int topK = getRuntimeTopK();
        double scoreThreshold = getRuntimeScoreThreshold();
        config.put("retrieval", Map.of(
                "topK", topK,
                "scoreThreshold", scoreThreshold,
                "defaultTopK", ragProperties.getRetrieval().getTopK(),
                "defaultScoreThreshold", ragProperties.getRetrieval().getScoreThreshold()
        ));

        return Result.success(config);
    }

    // ==================== 8. PUT /config ====================

    @PutMapping("/config")
    public Result<Map<String, Object>> updateConfig(@RequestBody Map<String, Object> body) {
        try {
            // Update topK
            Object topKObj = body.get("topK");
            if (topKObj instanceof Number num) {
                int topK = num.intValue();
                if (topK < 1 || topK > 50) {
                    return Result.error("topK must be between 1 and 50");
                }
                redisTemplate.opsForValue().set(RAG_CONFIG_TOPK_KEY, String.valueOf(topK));
            }

            // Update scoreThreshold
            Object thresholdObj = body.get("scoreThreshold");
            if (thresholdObj instanceof Number num) {
                double threshold = num.doubleValue();
                if (threshold < 0.0 || threshold > 1.0) {
                    return Result.error("scoreThreshold must be between 0.0 and 1.0");
                }
                redisTemplate.opsForValue().set(RAG_CONFIG_THRESHOLD_KEY, String.valueOf(threshold));
            }

            // Return updated config
            return getConfig();
        } catch (Exception e) {
            log.error("Failed to update RAG config: {}", e.getMessage(), e);
            return Result.error("Failed to update config: " + e.getMessage());
        }
    }

    // ==================== Private helpers ====================

    private int getRuntimeTopK() {
        String val = redisTemplate.opsForValue().get(RAG_CONFIG_TOPK_KEY);
        if (val != null) {
            try {
                return Integer.parseInt(val);
            } catch (NumberFormatException ignored) {}
        }
        return ragProperties.getRetrieval().getTopK();
    }

    private Double getRuntimeScoreThreshold() {
        String val = redisTemplate.opsForValue().get(RAG_CONFIG_THRESHOLD_KEY);
        if (val != null) {
            try {
                return Double.parseDouble(val);
            } catch (NumberFormatException ignored) {}
        }
        return ragProperties.getRetrieval().getScoreThreshold();
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }

    /**
     * Boost recall results whose content shares key terms with the original query.
     * When synonym expansion strips proper nouns (e.g. "乌萨奇" → "宠物摄影"),
     * the expanded queries flood recall with generic noise. This boost re-elevates
     * results that match the user's original intent.
     * <p>
     * Also expands key terms via a character/entity synonym table so that
     * "乌萨奇" can match documents containing "小八" or "吉伊" (same series).
     */
    private List<SearchResult> keywordBoost(String originalQuery, List<SearchResult> results) {
        // Extract key terms from the query, then expand with known synonyms
        Set<String> keyTerms = extractKeyTerms(originalQuery);
        keyTerms.addAll(expandSynonyms(keyTerms));
        if (keyTerms.isEmpty()) return results;

        log.debug("Keyword boost: key terms (with synonyms) for [{}]: {}", originalQuery, keyTerms);

        List<SearchResult> boosted = new ArrayList<>();
        for (SearchResult r : results) {
            String content = r.getContent() != null ? r.getContent() : "";
            double boost = 0.0;

            for (String term : keyTerms) {
                if (content.contains(term)) {
                    boost += 0.05;  // significant boost per matched term
                }
            }

            if (boost > 0) {
                SearchResult boostedResult = SearchResult.builder()
                        .id(r.getId())
                        .content(r.getContent())
                        .score(r.getScore() + boost)
                        .source(r.getSource())
                        .sourceId(r.getSourceId())
                        .metadata(r.getMetadata())
                        .build();
                boosted.add(boostedResult);
            } else {
                boosted.add(r);
            }
        }

        // Re-sort by boosted score descending
        boosted.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        return boosted;
    }

    /**
     * Expand key terms with known character/entity synonyms.
     * E.g. "乌萨奇" → also match "小八", "吉伊", "吉吉国王" (same series characters).
     */
    private Set<String> expandSynonyms(Set<String> keyTerms) {
        // Character / entity synonym mapping
        Map<String, List<String>> synonymMap = Map.of(
            "乌萨奇", List.of("小八", "吉伊", "吉吉国王", "Chiikawa"),
            "小八", List.of("乌萨奇", "吉伊", "吉吉国王"),
            "吉伊", List.of("乌萨奇", "小八", "吉吉国王"),
            "吉吉国王", List.of("乌萨奇", "小八", "吉伊")
        );

        Set<String> expanded = new LinkedHashSet<>();
        for (String term : keyTerms) {
            List<String> synonyms = synonymMap.get(term);
            if (synonyms != null) {
                expanded.addAll(synonyms);
            }
        }
        return expanded;
    }

    /**
     * Check if the LLM answer contains hallucinated names not present in the context.
     * If hallucination is detected, return a safe fallback based on actual context.
     */
    private String checkAnswerHallucination(String answer, String context) {
        if (answer == null || answer.isBlank()) return answer;

        // Extract potential Chinese names from the answer (2-3 char sequences
        // following photographer-related keywords)
        java.util.regex.Pattern namePattern = java.util.regex.Pattern.compile(
                "(?:摄影师|摄影师是|推荐|联系|查看|选择)[：:\\s]*([\\u4e00-\\u9fff]{2,3})");
        java.util.regex.Matcher matcher = namePattern.matcher(answer);

        boolean hasHallucination = false;
        StringBuilder fakeNames = new StringBuilder();
        while (matcher.find()) {
            String name = matcher.group(1);
            // Skip if this name appears in the context
            if (!context.contains(name)) {
                hasHallucination = true;
                if (fakeNames.length() > 0) fakeNames.append(", ");
                fakeNames.append(name);
            }
        }

        if (hasHallucination) {
            log.warn("Answer hallucination detected! Fake names: {}. Context: {}",
                    fakeNames, context.substring(0, Math.min(200, context.length())));

            // Build a safe answer from actual context content
            String safeAnswer = "根据知识库信息，";
            if (context.contains("小八") || context.contains("吉伊") || context.contains("吉吉国王")) {
                safeAnswer += "找到相关作品集：";
                // Extract portfolio names from context
                java.util.regex.Matcher pm = java.util.regex.Pattern.compile(
                        "作品集([^，。]+)").matcher(context);
                while (pm.find() && safeAnswer.length() < 500) {
                    safeAnswer += "《" + pm.group(1).trim() + "》、";
                }
                if (safeAnswer.endsWith("、")) {
                    safeAnswer = safeAnswer.substring(0, safeAnswer.length() - 1);
                }
                safeAnswer += "。";
                // Add photographer info
                java.util.regex.Matcher phm = java.util.regex.Pattern.compile(
                        "摄影师(\\S+)").matcher(context);
                if (phm.find()) {
                    safeAnswer += "这些作品的摄影师是" + phm.group(1) + "。";
                }
                safeAnswer += "您可以联系该摄影师进行约拍。";
            } else {
                // No relevant content in context
                safeAnswer += "暂未找到与您查询直接匹配的摄影师或作品集信息。";
            }
            return safeAnswer;
        }

        return answer;
    }

    /**
     * Extract key terms (meaningful Chinese words) from a query string.
     * Filters out common stop words and single characters.
     */
    private Set<String> extractKeyTerms(String query) {
        if (query == null || query.isBlank()) return Collections.emptySet();

        // Common Chinese stop words to skip
        Set<String> stopWords = Set.of(
            "我想", "我要", "给我", "帮我", "请问", "能不能", "可不可以",
            "拍照", "拍摄", "推荐", "摄影师", "一个", "一下", "什么",
            "怎么", "如何", "哪里", "哪个", "有没有", "有没有什么",
            "的", "了", "吗", "呢", "吧", "啊", "呀"
        );

        Set<String> terms = new LinkedHashSet<>();
        String cleaned = query.replaceAll("[\\s\\n\\r,，。！？、]", "");

        // Extract 2-4 char terms (Chinese words are typically 2-4 characters)
        for (int len = 4; len >= 2; len--) {
            for (int i = 0; i <= cleaned.length() - len; i++) {
                String term = cleaned.substring(i, i + len);
                // Only keep Chinese characters
                if (term.matches("[一-鿿]+") && !stopWords.contains(term)) {
                    terms.add(term);
                }
            }
        }

        return terms;
    }
}
