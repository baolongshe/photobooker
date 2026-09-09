package com.xhxi.photobooker.service.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Unified search result DTO for merging results from multiple retrieval sources
 * (Qdrant vector search, Elasticsearch keyword search).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult {
    private String id;
    private String content;
    private double score;
    private String source;        // "photographer", "package", "portfolio"
    private Long sourceId;
    private Map<String, Object> metadata;
}
