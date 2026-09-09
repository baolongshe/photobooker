package com.xhxi.photobooker.agent.tools;

import com.xhxi.photobooker.agent.AgentTool;
import com.xhxi.photobooker.config.RagProperties;
import com.xhxi.photobooker.service.ollama.OllamaClient;
import com.xhxi.photobooker.service.rag.QdrantVectorService;
import com.xhxi.photobooker.service.rag.SearchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Tool that searches the RAG knowledge base (Qdrant vector store).
 * Used by the AI agent to find relevant portfolios, packages, and
 * photographers via semantic search when exact DB queries fail.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "rag.enabled", havingValue = "true")
public class SearchKnowledgeBaseTool implements AgentTool {

    @Autowired
    private OllamaClient ollamaClient;

    @Autowired
    private QdrantVectorService qdrantVectorService;

    @Autowired
    private RagProperties ragProperties;

    @Override
    public String getName() {
        return "searchKnowledgeBase";
    }

    @Override
    public String getDescription() {
        return "在平台知识库中语义搜索，适用于模糊查询（如用角色名、风格描述、场景需求搜索）。"
                + "当searchPhotographer和searchPackages无法找到结果时，可用此工具进行兜底搜索。"
                + "返回匹配的摄影师、作品集或套餐信息";
    }

    @Override
    public Map<String, Object> getParameterSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("query", "string - 搜索查询文本（可以是角色名、风格描述、场景需求等自然语言）");
        schema.put("topK", "number - 返回结果数量（默认5，最大10）");
        return schema;
    }

    @Override
    public Object execute(Map<String, Object> parameters) throws Exception {
        String query = (String) parameters.get("query");
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        int topK = 5;
        if (parameters.containsKey("topK")) {
            topK = Integer.parseInt(parameters.get("topK").toString());
            topK = Math.max(1, Math.min(topK, 10));
        }

        Double scoreThreshold = ragProperties.getRetrieval().getScoreThreshold();

        log.info("Knowledge base search: query=[{}], topK={}, threshold={}", query, topK, scoreThreshold);

        try {
            // Generate embedding and search
            float[] embedding = ollamaClient.generateEmbedding(query);
            List<SearchResult> results = qdrantVectorService.search(embedding, topK, scoreThreshold);

            // Also try with expanded keyword: if query contains specific terms,
            // do an additional search without the threshold for better recall
            if (results.isEmpty() && scoreThreshold != null && scoreThreshold > 0.3) {
                log.info("No results with threshold {}, retrying without threshold", scoreThreshold);
                results = qdrantVectorService.search(embedding, topK, null);
            }

            log.info("Knowledge base search returned {} results", results.size());

            return results.stream().map(r -> {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("content", truncate(r.getContent(), 300));
                map.put("source", r.getSource());
                map.put("sourceId", r.getSourceId());
                map.put("score", Math.round(r.getScore() * 100.0) / 100.0);
                if (r.getMetadata() != null) {
                    // Pass through useful metadata fields
                    Map<String, Object> cleanMeta = new LinkedHashMap<>();
                    String[] usefulKeys = {"title", "name", "photographerId", "photographerName"};
                    for (String key : usefulKeys) {
                        Object val = r.getMetadata().get(key);
                        if (val != null) {
                            cleanMeta.put(key, val);
                        }
                    }
                    if (!cleanMeta.isEmpty()) {
                        map.put("metadata", cleanMeta);
                    }
                }
                return map;
            }).collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Knowledge base search failed for [{}]: {}", query, e.getMessage());
            return Collections.emptyList();
        }
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }
}
