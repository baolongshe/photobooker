package com.xhxi.photobooker.service.rag;

import com.xhxi.photobooker.config.RagProperties;
import com.xhxi.photobooker.service.SpringAiChatService;
import com.xhxi.photobooker.service.ollama.OllamaClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Main RAG pipeline orchestrator.
 * Coordinates query rewriting, multi-path recall, reranking, and LLM generation.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "rag.enabled", havingValue = "true")
public class RagPipelineService {

    private final QueryRewriteService queryRewriteService;
    private final MultiPathRecallService multiPathRecallService;
    private final RerankService rerankService;
    private final SpringAiChatService springAiChatService;
    private final OllamaClient ollamaClient;
    private final RagProperties ragProperties;

    private static final String RAG_SYSTEM_PROMPT_TEMPLATE = """
            你是PhotoBooker摄影约拍平台的智能助手。请根据以下知识库信息回答用户问题。
            如果知识库中没有相关信息，请诚实告知用户你不确定，不要编造信息。

            参考信息：
            %s
            """;

    public RagPipelineService(QueryRewriteService queryRewriteService,
                               MultiPathRecallService multiPathRecallService,
                               RerankService rerankService,
                               SpringAiChatService springAiChatService,
                               OllamaClient ollamaClient,
                               RagProperties ragProperties) {
        this.queryRewriteService = queryRewriteService;
        this.multiPathRecallService = multiPathRecallService;
        this.rerankService = rerankService;
        this.springAiChatService = springAiChatService;
        this.ollamaClient = ollamaClient;
        this.ragProperties = ragProperties;
    }

    /**
     * Execute the full RAG pipeline and return a streaming response.
     *
     * @param question    the user's question
     * @param chatHistory optional chat history for context
     * @return Flux of response string chunks for SSE streaming
     */
    public Flux<String> ragQuery(String question, String chatHistory) {
        log.info("Starting RAG pipeline for question: [{}]", question);

        try {
            // Step 1: Query Rewrite
            QueryRewriteService.RewriteResult rewriteResult =
                    queryRewriteService.rewrite(question, chatHistory);

            // Step 2: Collect all queries (deduplicate)
            List<String> allQueries = collectQueries(rewriteResult);
            log.info("Collected {} unique queries for recall", allQueries.size());

            // Step 3: Multi-path Recall
            int topK = ragProperties.getRetrieval().getTopK();
            Double scoreThreshold = ragProperties.getRetrieval().getScoreThreshold();
            List<SearchResult> candidates = multiPathRecallService.recall(allQueries, topK * 2, scoreThreshold);
            log.info("Recall phase returned {} candidates", candidates.size());

            // Step 4: Rerank
            List<SearchResult> reranked = rerankService.rerank(question, candidates);
            log.info("Rerank phase returned {} results", reranked.size());

            // Step 5: Build context from top results
            String context = buildContext(reranked, topK);

            // Step 6: Build system prompt
            String systemPrompt = String.format(RAG_SYSTEM_PROMPT_TEMPLATE, context);

            // Step 7: Stream LLM response (ModelScope DeepSeek via Spring AI, fallback to Ollama on failure)
            log.info("Starting LLM streaming with context ({} chars)", context.length());
            return springAiChatService.streamChat(systemPrompt, question)
                    .onErrorResume(e -> {
                        log.warn("ModelScope DeepSeek failed ({}), falling back to Ollama", e.getMessage());
                        return ollamaClient.chatCompletionStream(systemPrompt, question);
                    })
                    .doOnError(e -> log.error("RAG streaming error: {}", e.getMessage()))
                    .doOnComplete(() -> log.info("RAG streaming completed for question: [{}]", question));

        } catch (Exception e) {
            log.error("RAG pipeline failed for question [{}]: {}", question, e.getMessage(), e);
            return Flux.just("抱歉，处理您的问题时出现了错误，请稍后再试。");
        }
    }

    /**
     * Execute the full RAG pipeline and return debug information about each stage.
     *
     * @param question    the user's question
     * @param chatHistory optional chat history
     * @return map containing all intermediate results and final answer
     */
    public Map<String, Object> ragQueryDebug(String question, String chatHistory) {
        log.info("Starting RAG pipeline (debug mode) for question: [{}]", question);
        Map<String, Object> debugResult = new LinkedHashMap<>();

        try {
            // Step 1: Query Rewrite
            QueryRewriteService.RewriteResult rewriteResult =
                    queryRewriteService.rewrite(question, chatHistory);

            Map<String, Object> rewriteDebug = new LinkedHashMap<>();
            rewriteDebug.put("originalQuery", rewriteResult.getOriginalQuery());
            rewriteDebug.put("compensatedQuery", rewriteResult.getCompensatedQuery());
            rewriteDebug.put("expandedQueries", rewriteResult.getExpandedQueries());
            rewriteDebug.put("subQueries", rewriteResult.getSubQueries());
            debugResult.put("rewrite", rewriteDebug);

            // Step 2: Collect all queries
            List<String> allQueries = collectQueries(rewriteResult);
            debugResult.put("allQueries", allQueries);

            // Step 3: Multi-path Recall
            int topK = ragProperties.getRetrieval().getTopK();
            Double scoreThreshold = ragProperties.getRetrieval().getScoreThreshold();
            List<SearchResult> candidates = multiPathRecallService.recall(allQueries, topK * 2, scoreThreshold);

            List<Map<String, Object>> recallDebug = candidates.stream()
                    .map(sr -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("id", sr.getId());
                        m.put("content", sr.getContent());
                        m.put("score", sr.getScore());
                        m.put("source", sr.getSource());
                        m.put("sourceId", sr.getSourceId());
                        return m;
                    })
                    .collect(Collectors.toList());
            debugResult.put("recall", recallDebug);

            // Step 4: Rerank
            List<SearchResult> reranked = rerankService.rerank(question, candidates);

            List<Map<String, Object>> rerankDebug = reranked.stream()
                    .map(sr -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("id", sr.getId());
                        m.put("content", sr.getContent());
                        m.put("score", sr.getScore());
                        m.put("source", sr.getSource());
                        return m;
                    })
                    .collect(Collectors.toList());
            debugResult.put("rerank", rerankDebug);

            // Step 5: Build context
            String context = buildContext(reranked, topK);
            debugResult.put("contextLength", context.length());

            // Step 6: Build system prompt and get final answer (non-streaming for debug)
            String systemPrompt = String.format(RAG_SYSTEM_PROMPT_TEMPLATE, context);
            String finalAnswer;
            try {
                finalAnswer = springAiChatService.streamChat(systemPrompt, question)
                        .blockLast(Duration.ofMinutes(2));
            } catch (Exception e) {
                log.warn("ModelScope DeepSeek failed ({}), falling back to Ollama", e.getMessage());
                finalAnswer = ollamaClient.chatCompletion(systemPrompt, question);
            }
            debugResult.put("finalAnswer", finalAnswer);
            debugResult.put("status", "success");

        } catch (Exception e) {
            log.error("RAG debug pipeline failed: {}", e.getMessage(), e);
            debugResult.put("status", "error");
            debugResult.put("error", e.getMessage());
        }

        return debugResult;
    }

    /**
     * Collect all query variants from rewrite result, deduplicating.
     */
    private List<String> collectQueries(QueryRewriteService.RewriteResult rewriteResult) {
        Set<String> querySet = new LinkedHashSet<>();

        // Add compensated query
        if (rewriteResult.getCompensatedQuery() != null && !rewriteResult.getCompensatedQuery().isBlank()) {
            querySet.add(rewriteResult.getCompensatedQuery());
        }

        // Add expanded queries
        if (rewriteResult.getExpandedQueries() != null) {
            querySet.addAll(rewriteResult.getExpandedQueries());
        }

        // Add sub queries
        if (rewriteResult.getSubQueries() != null) {
            querySet.addAll(rewriteResult.getSubQueries());
        }

        // Always include original query
        if (rewriteResult.getOriginalQuery() != null) {
            querySet.add(rewriteResult.getOriginalQuery());
        }

        return new ArrayList<>(querySet);
    }

    /**
     * Build context string from top search results.
     */
    private String buildContext(List<SearchResult> results, int maxResults) {
        if (results == null || results.isEmpty()) {
            return "（未找到相关知识库信息）";
        }

        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (SearchResult sr : results) {
            if (count >= maxResults) break;
            if (sr.getContent() != null && !sr.getContent().isBlank()) {
                sb.append("[").append(count + 1).append("] ");
                sb.append(sr.getContent());
                if (sr.getSource() != null) {
                    sb.append(" (来源: ").append(sr.getSource()).append(")");
                }
                sb.append("\n\n");
                count++;
            }
        }

        return sb.toString().trim();
    }
}
