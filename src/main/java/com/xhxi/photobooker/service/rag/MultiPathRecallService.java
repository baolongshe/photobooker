package com.xhxi.photobooker.service.rag;

import com.xhxi.photobooker.config.RagProperties;
import com.xhxi.photobooker.service.ollama.OllamaClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Multi-path recall service that runs vector search and keyword search in parallel
 * for multiple queries, then merges results using RRF.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "rag.enabled", havingValue = "true")
public class MultiPathRecallService {

    private final OllamaClient ollamaClient;
    private final QdrantVectorService qdrantVectorService;
    private final ElasticsearchService elasticsearchService;
    private final ResultMerger resultMerger;
    private final RagProperties ragProperties;

    private static final ExecutorService recallExecutor = new ThreadPoolExecutor(
            2, 8, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100),
            new ThreadPoolExecutor.CallerRunsPolicy());

    @PreDestroy
    public void shutdown() {
        recallExecutor.shutdown();
        try {
            if (!recallExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                recallExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            recallExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public MultiPathRecallService(OllamaClient ollamaClient,
                                   QdrantVectorService qdrantVectorService,
                                   ElasticsearchService elasticsearchService,
                                   ResultMerger resultMerger,
                                   RagProperties ragProperties) {
        this.ollamaClient = ollamaClient;
        this.qdrantVectorService = qdrantVectorService;
        this.elasticsearchService = elasticsearchService;
        this.resultMerger = resultMerger;
        this.ragProperties = ragProperties;
    }

    /**
     * Execute multi-path recall for multiple queries in parallel.
     *
     * @param queries       list of query strings (rewritten variants)
     * @param topK          number of top results to return
     * @param scoreThreshold minimum score threshold (can be null)
     * @return merged, deduplicated, sorted list of search results
     */
    public List<SearchResult> recall(List<String> queries, int topK, Double scoreThreshold) {
        if (queries == null || queries.isEmpty()) {
            log.warn("No queries provided for recall");
            return Collections.emptyList();
        }

        log.info("Starting multi-path recall for {} queries, topK={}", queries.size(), topK);

        boolean esEnabled = ragProperties.getElasticsearch().isEnabled();

        // For each query, run vector (+ keyword if ES enabled) search in parallel
        List<CompletableFuture<List<SearchResult>>> futures = new ArrayList<>();

        for (String query : queries) {
            // Vector search future
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

            // Keyword search future (only when ES is enabled)
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

        // Wait for all futures to complete
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0]));

        try {
            allFutures.get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Multi-path recall timed out or failed: {}", e.getMessage());
        }

        // Collect all vector and keyword results per query, then merge
        List<SearchResult> allMerged = new ArrayList<>();

        int index = 0;
        for (String query : queries) {
            try {
                List<SearchResult> vectorResults = futures.get(index).getNow(Collections.emptyList());
                List<SearchResult> keywordResults = esEnabled
                        ? futures.get(index + 1).getNow(Collections.emptyList())
                        : Collections.emptyList();
                List<SearchResult> merged = resultMerger.merge(vectorResults, keywordResults, topK);
                allMerged.addAll(merged);
            } catch (Exception e) {
                log.error("Failed to collect results for query [{}]: {}", query, e.getMessage());
            }
            index += esEnabled ? 2 : 1;
        }

        // Deduplicate across queries by id, keeping highest score
        Map<String, SearchResult> deduped = new LinkedHashMap<>();
        for (SearchResult sr : allMerged) {
            if (!deduped.containsKey(sr.getId()) || sr.getScore() > deduped.get(sr.getId()).getScore()) {
                deduped.put(sr.getId(), sr);
            }
        }

        // Sort by score descending and return top K
        List<SearchResult> finalResults = deduped.values().stream()
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(topK)
                .collect(Collectors.toList());

        log.info("Multi-path recall completed: {} total results -> {} after dedup",
                allMerged.size(), finalResults.size());
        return finalResults;
    }

    private List<SearchResult> convertEsResults(List<ElasticsearchService.SearchResult> esResults) {
        return esResults.stream()
                .map(r -> SearchResult.builder()
                        .id(r.id())
                        .content(r.content())
                        .score(r.score())
                        .source(getStringFromMeta(r.metadata(), "source"))
                        .sourceId(getLongFromMeta(r.metadata(), "sourceId"))
                        .metadata(r.metadata())
                        .build())
                .collect(Collectors.toList());
    }

    private String getStringFromMeta(Map<String, Object> meta, String key) {
        if (meta == null) return null;
        Object val = meta.get(key);
        return val != null ? val.toString() : null;
    }

    private Long getLongFromMeta(Map<String, Object> meta, String key) {
        if (meta == null) return null;
        Object val = meta.get(key);
        if (val instanceof Long l) return l;
        if (val instanceof Number n) return n.longValue();
        if (val instanceof String s) {
            try { return Long.parseLong(s); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }
}
