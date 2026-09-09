package com.xhxi.photobooker.service.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Reciprocal Rank Fusion (RRF) algorithm for merging search results
 * from multiple retrieval paths (vector + keyword).
 */
@Slf4j
@Service
public class ResultMerger {

    private static final int RRF_K = 60; // standard RRF constant

    /**
     * Merge vector and keyword search results using RRF algorithm.
     *
     * @param vectorResults  results from Qdrant vector search
     * @param keywordResults results from Elasticsearch keyword search
     * @param topK           number of top results to return
     * @return merged, deduplicated, sorted list of top K results
     */
    public List<SearchResult> merge(List<SearchResult> vectorResults,
                                     List<SearchResult> keywordResults,
                                     int topK) {
        if (vectorResults == null) vectorResults = Collections.emptyList();
        if (keywordResults == null) keywordResults = Collections.emptyList();

        // Map: id -> accumulated RRF score
        Map<String, Double> rrfScores = new LinkedHashMap<>();
        // Map: id -> the SearchResult (keep first occurrence)
        Map<String, SearchResult> resultMap = new LinkedHashMap<>();

        // Score vector results by rank (1-based)
        for (int i = 0; i < vectorResults.size(); i++) {
            SearchResult sr = vectorResults.get(i);
            int rank = i + 1;
            double rrfScore = 1.0 / (RRF_K + rank);
            rrfScores.merge(sr.getId(), rrfScore, Double::sum);
            resultMap.putIfAbsent(sr.getId(), sr);
        }

        // Score keyword results by rank (1-based)
        for (int i = 0; i < keywordResults.size(); i++) {
            SearchResult sr = keywordResults.get(i);
            int rank = i + 1;
            double rrfScore = 1.0 / (RRF_K + rank);
            rrfScores.merge(sr.getId(), rrfScore, Double::sum);
            resultMap.putIfAbsent(sr.getId(), sr);
        }

        // Sort by RRF score descending
        List<Map.Entry<String, Double>> sorted = new ArrayList<>(rrfScores.entrySet());
        sorted.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        // Build final result list with RRF score as the score
        List<SearchResult> merged = new ArrayList<>();
        for (Map.Entry<String, Double> entry : sorted) {
            if (merged.size() >= topK) break;
            SearchResult original = resultMap.get(entry.getKey());
            SearchResult merged_result = SearchResult.builder()
                    .id(original.getId())
                    .content(original.getContent())
                    .score(entry.getValue())
                    .source(original.getSource())
                    .sourceId(original.getSourceId())
                    .metadata(original.getMetadata())
                    .build();
            merged.add(merged_result);
        }

        log.debug("RRF merge: {} vector + {} keyword -> {} merged results",
                vectorResults.size(), keywordResults.size(), merged.size());
        return merged;
    }
}
