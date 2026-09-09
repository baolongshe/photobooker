package com.xhxi.photobooker.service.rag;

import com.xhxi.photobooker.config.RagProperties;
import com.xhxi.photobooker.entity.rag.KnowledgeDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.stereotype.Service;

import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@ConditionalOnProperty(name = "rag.enabled", havingValue = "true", matchIfMissing = false)
public class ElasticsearchService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final RagProperties ragProperties;
    private final boolean available;

    public ElasticsearchService(@Autowired(required = false) ElasticsearchOperations elasticsearchOperations,
                                RagProperties ragProperties) {
        this.elasticsearchOperations = elasticsearchOperations;
        this.ragProperties = ragProperties;
        this.available = ragProperties.getElasticsearch().isEnabled() && elasticsearchOperations != null;
        if (!available) {
            log.info("Elasticsearch is not enabled/available - ES features will be disabled");
        }
    }

    public boolean isAvailable() {
        return available;
    }

    public void indexDocument(KnowledgeDocument doc) {
        if (!available) {
            log.debug("ES not available, skipping index for doc: {}", doc.getId());
            return;
        }
        try {
            IndexQuery indexQuery = new IndexQueryBuilder()
                    .withId(doc.getId())
                    .withObject(doc)
                    .build();
            elasticsearchOperations.index(indexQuery,
                    IndexCoordinates.of(ragProperties.getElasticsearch().getIndexName()));
            log.info("Indexed document to Elasticsearch: id={}", doc.getId());
        } catch (Exception e) {
            log.error("Failed to index document to Elasticsearch: {}", e.getMessage());
        }
    }

    public List<SearchResult> search(String queryText, int topK) {
        if (!available) {
            return Collections.emptyList();
        }
        try {
            Query multiMatchQuery = MultiMatchQuery.of(m -> m
                    .query(queryText)
                    .fields("content", "content^2", "source")
            )._toQuery();

            NativeQuery query = NativeQuery.builder()
                    .withQuery(multiMatchQuery)
                    .withPageable(PageRequest.of(0, topK))
                    .build();

            SearchHits<KnowledgeDocument> hits = elasticsearchOperations.search(
                    query, KnowledgeDocument.class,
                    IndexCoordinates.of(ragProperties.getElasticsearch().getIndexName()));

            List<SearchResult> results = hits.getSearchHits().stream()
                    .map(hit -> new SearchResult(
                            hit.getId(),
                            hit.getContent().getContent(),
                            hit.getScore(),
                            Map.of(
                                    "source", hit.getContent().getSource() != null ? hit.getContent().getSource() : "",
                                    "sourceId", hit.getContent().getSourceId() != null ? hit.getContent().getSourceId() : 0L
                            )
                    ))
                    .collect(Collectors.toList());

            log.debug("Elasticsearch search returned {} results", results.size());
            return results;
        } catch (Exception e) {
            log.error("Failed to search Elasticsearch: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public void deleteDocument(String docId) {
        if (!available) return;
        try {
            elasticsearchOperations.delete(docId,
                    IndexCoordinates.of(ragProperties.getElasticsearch().getIndexName()));
            log.info("Deleted document from Elasticsearch: id={}", docId);
        } catch (Exception e) {
            log.error("Failed to delete document from Elasticsearch: {}", e.getMessage());
        }
    }

    public Map<String, Object> getIndexInfo() {
        Map<String, Object> info = new HashMap<>();
        String indexName = ragProperties.getElasticsearch().getIndexName();
        info.put("indexName", indexName);

        if (!available) {
            info.put("health", "disabled");
            info.put("totalDocs", 0L);
            return info;
        }
        try {
            NativeQuery countQuery = NativeQuery.builder()
                    .withQuery(q -> q.matchAll(m -> m))
                    .withPageable(PageRequest.of(0, 0))
                    .build();

            SearchHits<KnowledgeDocument> hits = elasticsearchOperations.search(
                    countQuery, KnowledgeDocument.class,
                    IndexCoordinates.of(indexName));

            info.put("totalDocs", hits.getTotalHits());
            info.put("health", "available");
            return info;
        } catch (Exception e) {
            log.error("Failed to get ES index info: {}", e.getMessage());
            info.put("health", "unavailable");
            info.put("error", e.getMessage());
            return info;
        }
    }

    public List<KnowledgeDocument> getAllDocuments(int page, int size) {
        if (!available) return Collections.emptyList();
        try {
            NativeQuery query = NativeQuery.builder()
                    .withQuery(q -> q.matchAll(m -> m))
                    .withPageable(PageRequest.of(page, size))
                    .build();

            SearchHits<KnowledgeDocument> hits = elasticsearchOperations.search(
                    query, KnowledgeDocument.class,
                    IndexCoordinates.of(ragProperties.getElasticsearch().getIndexName()));

            return hits.getSearchHits().stream()
                    .map(SearchHit::getContent)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to get all documents from ES: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public record SearchResult(String id, String content, float score, Map<String, Object> metadata) {}
}
