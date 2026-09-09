package com.xhxi.photobooker.service.rag;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.xhxi.photobooker.config.RagProperties;
import com.xhxi.photobooker.service.ollama.OllamaClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * LLM-based reranking service.
 * Uses the configured Ollama chat model to rerank candidate documents by relevance.
 * Automatically skips reranking when using small models (&lt; 3B params) that
 * cannot reliably rank documents.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "rag.enabled", havingValue = "true")
public class RerankService {

    private final OllamaClient ollamaClient;
    private final RagProperties ragProperties;

    /** Max candidates to send to LLM for reranking (small models can't handle more) */
    private static final int MAX_RERANK_CANDIDATES = 10;

    private static final String RERANK_SYSTEM_PROMPT = """
            你是一个文档相关性评估专家。给定一个查询和一组文档，请按照与查询的相关性从高到低排序。
            请严格按照以下JSON格式返回排序结果（只返回JSON，不要其他内容）：
            [{"index": 原始索引, "score": 相关性评分0-10}, ...]
            """;

    public RerankService(OllamaClient ollamaClient, RagProperties ragProperties) {
        this.ollamaClient = ollamaClient;
        this.ragProperties = ragProperties;
    }

    /**
     * Rerank candidate search results using LLM.
     *
     * @param query      the original user query
     * @param candidates candidate search results from recall phase
     * @return reranked list of search results (same objects, reordered)
     */
    public List<SearchResult> rerank(String query, List<SearchResult> candidates) {
        if (candidates == null || candidates.size() <= 1) {
            return candidates;
        }

        // Skip rerank if the chat model is too small to produce reliable rankings
        if (isWeakModel()) {
            log.info("Rerank skipped: weak model detected ({})", ragProperties.getOllama().getChatModel());
            return candidates;
        }

        // Limit candidates to avoid overwhelming small models
        List<SearchResult> limitedCandidates = candidates;
        if (candidates.size() > MAX_RERANK_CANDIDATES) {
            limitedCandidates = new ArrayList<>(candidates.subList(0, MAX_RERANK_CANDIDATES));
            log.debug("Rerank limited to {} candidates (from {})", MAX_RERANK_CANDIDATES, candidates.size());
        }

        try {
            // Build user message with numbered documents
            StringBuilder userMessage = new StringBuilder();
            userMessage.append("查询：").append(query).append("\n\n文档列表：\n");
            for (int i = 0; i < limitedCandidates.size(); i++) {
                String content = limitedCandidates.get(i).getContent();
                if (content != null && content.length() > 500) {
                    content = content.substring(0, 500) + "...";
                }
                userMessage.append(i).append(": ").append(content).append("\n");
            }

            log.debug("Reranking {} candidates for query: [{}]", limitedCandidates.size(), query);

            String response = ollamaClient.chatCompletion(RERANK_SYSTEM_PROMPT, userMessage.toString());
            log.debug("Rerank LLM response: [{}]", response);

            // Parse JSON response
            List<RerankEntry> entries = parseRerankResponse(response, limitedCandidates.size());

            if (entries.isEmpty()) {
                log.warn("Rerank parsing returned empty result, returning original order");
                return candidates;
            }

            // Reorder limited candidates based on LLM ranking
            List<SearchResult> reranked = new ArrayList<>();
            for (RerankEntry entry : entries) {
                if (entry.index >= 0 && entry.index < limitedCandidates.size()) {
                    SearchResult original = limitedCandidates.get(entry.index);
                    SearchResult reranked_result = SearchResult.builder()
                            .id(original.getId())
                            .content(original.getContent())
                            .score(entry.score / 10.0)
                            .source(original.getSource())
                            .sourceId(original.getSourceId())
                            .metadata(original.getMetadata())
                            .build();
                    reranked.add(reranked_result);
                }
            }

            // Add any limited candidates that were missed by the LM
            Set<Integer> includedIndices = entries.stream()
                    .map(e -> e.index)
                    .collect(Collectors.toSet());
            for (int i = 0; i < limitedCandidates.size(); i++) {
                if (!includedIndices.contains(i)) {
                    reranked.add(limitedCandidates.get(i));
                }
            }

            // Append remaining candidates that were trimmed (keep original order)
            if (candidates.size() > MAX_RERANK_CANDIDATES) {
                for (int i = MAX_RERANK_CANDIDATES; i < candidates.size(); i++) {
                    reranked.add(candidates.get(i));
                }
            }

            log.info("Reranking completed: {} candidates -> {} reranked results",
                    candidates.size(), reranked.size());

            if (!isRerankValid(reranked)) {
                log.warn("Rerank results appear unreliable, keeping original recall order");
                return candidates;
            }

            return reranked;

        } catch (Exception e) {
            log.error("Reranking failed for query [{}]: {}", query, e.getMessage());
            return candidates; // return original order on failure
        }
    }

    /**
     * Check if rerank results are valid. Small/weak models often produce nonsensical scores.
     * Returns false if scores indicate the rerank is unreliable.
     */
    private boolean isRerankValid(List<SearchResult> reranked) {
        if (reranked == null || reranked.isEmpty()) return false;

        // Check 1: Scores should be in [0, 1] range (normalized from 0-10)
        double maxScore = reranked.stream()
                .mapToDouble(SearchResult::getScore)
                .max().orElse(0);
        if (maxScore > 1.0) {
            log.warn("Rerank invalid: max score {} exceeds 1.0", maxScore);
            return false;
        }

        // Check 2: Count how many items were actually reranked (score != original vector score)
        // Original vector scores are typically 0.01-0.02. Reranked scores should be 0-1.
        long rerankedCount = reranked.stream()
                .mapToDouble(SearchResult::getScore)
                .filter(s -> s >= 0.05)  // scores below 0.05 are likely untouched original scores
                .count();
        if (rerankedCount < Math.min(3, reranked.size() / 2.0)) {
            log.warn("Rerank invalid: only {}/{} items were actually reranked", rerankedCount, reranked.size());
            return false;
        }

        // Check 3: If too many items have the exact same score → the model can't differentiate
        Map<Double, Long> scoreBuckets = new java.util.HashMap<>();
        for (SearchResult r : reranked) {
            double bucket = Math.round(r.getScore() * 100.0) / 100.0; // round to 2 decimal places
            scoreBuckets.merge(bucket, 1L, Long::sum);
        }
        long maxInBucket = scoreBuckets.values().stream().mapToLong(Long::longValue).max().orElse(0);
        if (maxInBucket > reranked.size() * 0.5) {
            log.warn("Rerank invalid: {} items share the same score bucket (undifferentiated)", maxInBucket);
            return false;
        }

        return true;
    }

    private List<RerankEntry> parseRerankResponse(String response, int expectedCount) {
        try {
            // Try to extract JSON array from response (may contain markdown code blocks)
            String jsonStr = extractJsonArray(response);
            if (jsonStr == null) {
                log.warn("Could not extract JSON array from rerank response");
                return Collections.emptyList();
            }

            JSONArray jsonArray = JSON.parseArray(jsonStr);
            List<RerankEntry> entries = new ArrayList<>();

            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                int index = obj.getIntValue("index");
                double score = obj.getDoubleValue("score");
                entries.add(new RerankEntry(index, score));
            }

            // Sort by score descending
            entries.sort((a, b) -> Double.compare(b.score, a.score));
            return entries;

        } catch (Exception e) {
            log.error("Failed to parse rerank response: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private String extractJsonArray(String response) {
        if (response == null || response.isBlank()) return null;

        String trimmed = response.trim();

        // Try direct parse first
        if (trimmed.startsWith("[")) {
            return trimmed;
        }

        // Try to find JSON array in markdown code blocks
        int startIdx = trimmed.indexOf('[');
        int endIdx = trimmed.lastIndexOf(']');
        if (startIdx >= 0 && endIdx > startIdx) {
            return trimmed.substring(startIdx, endIdx + 1);
        }

        return null;
    }

    /**
     * Check if the configured chat model is too small for reliable reranking.
     * Models under ~3B parameters tend to produce random/unreliable rankings.
     */
    private boolean isWeakModel() {
        String model = ragProperties.getOllama().getChatModel();
        if (model == null) return true;

        String lower = model.toLowerCase();
        // Tiny models that can't do reliable ranking
        return lower.contains("1.5b") || lower.contains("0.5b")
                || lower.contains("1.8b") || lower.contains("2b")
                || lower.contains("tiny") || lower.contains("mini");
    }

    private record RerankEntry(int index, double score) {}
}
