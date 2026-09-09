package com.xhxi.photobooker.service.rag;

import com.xhxi.photobooker.config.RagProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.util.*;

/**
 * Qdrant vector store service using REST API (WebClient).
 * Avoids gRPC/protobuf dependency conflicts.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "rag.enabled", havingValue = "true")
public class QdrantVectorService {

    private final WebClient webClient;
    private final String collectionName;
    private final int vectorSize;

    public QdrantVectorService(RagProperties ragProperties, WebClient.Builder webClientBuilder) {
        RagProperties.QdrantProperties qdrantProps = ragProperties.getQdrant();
        // Use HTTP REST port (default 6333), not gRPC port
        String baseUrl = "http://" + qdrantProps.getHost() + ":" + getRestPort(qdrantProps.getPort());
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.collectionName = qdrantProps.getCollectionName();
        this.vectorSize = qdrantProps.getVectorSize();
        log.info("QdrantVectorService initialized with REST API at {}, vector size {}", baseUrl, vectorSize);
    }

    @PostConstruct
    public void init() {
        try {
            ensureCollectionExists();
        } catch (Exception e) {
            log.warn("Failed to connect to Qdrant or create collection: {}. " +
                    "Qdrant features may not work until connection is established.", e.getMessage());
        }
    }

    private int getRestPort(int grpcPort) {
        // Qdrant default: gRPC=6334, REST=6333. If user configured 6334, use 6333 for REST
        if (grpcPort == 6334) return 6333;
        return grpcPort;
    }

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
            // Collection doesn't exist or connection error, try to create
            log.info("Qdrant collection '{}' not found, creating...", collectionName);
            createCollection();
        }
    }

    /**
     * Extract configured vector size from Qdrant collection info response.
     * Returns -1 if the structure is unexpected.
     */
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

    private void createCollection() {
        try {
            Map<String, Object> request = new HashMap<>();
            Map<String, Object> vectors = new HashMap<>();
            vectors.put("size", vectorSize);
            vectors.put("distance", "Cosine");
            request.put("vectors", vectors);

            webClient.put()
                    .uri("/collections/" + collectionName)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            log.info("Qdrant collection '{}' created successfully", collectionName);
        } catch (Exception e) {
            log.error("Failed to create Qdrant collection: {}", e.getMessage());
        }
    }

    public void upsertDocument(String id, String content, float[] embedding, Map<String, Object> metadata) {
        try {
            // Convert id to UUID format for Qdrant
            String pointId = toQdrantId(id);

            Map<String, Object> point = new HashMap<>();
            point.put("id", pointId);
            point.put("vector", embedding);

            Map<String, Object> payload = new HashMap<>(metadata != null ? metadata : Map.of());
            payload.put("content", content);
            point.put("payload", payload);

            Map<String, Object> request = new HashMap<>();
            request.put("points", List.of(point));

            webClient.put()
                    .uri("/collections/" + collectionName + "/points")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            log.debug("Upserted document to Qdrant: id={}", id);
        } catch (Exception e) {
            log.error("Failed to upsert document to Qdrant: {}", e.getMessage());
            throw new RuntimeException("Failed to upsert to Qdrant", e);
        }
    }

    public List<SearchResult> search(float[] queryEmbedding, int topK, Double scoreThreshold) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("vector", queryEmbedding);
            request.put("limit", topK);
            request.put("with_payload", true);
            request.put("with_vector", false);
            if (scoreThreshold != null) {
                request.put("score_threshold", scoreThreshold);
            }

            Map<String, Object> response = webClient.post()
                    .uri("/collections/" + collectionName + "/points/search")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            if (response == null) return Collections.emptyList();

            // Qdrant v1.8+ REST API: search returns result as a JSON array directly
            // Older versions / scroll API: result is an object with a "points" key
            Object resultObj = response.get("result");
            if (resultObj == null) return Collections.emptyList();

            List<Map<String, Object>> points;
            if (resultObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> list = (List<Map<String, Object>>) resultObj;
                points = list;
            } else if (resultObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> resultMap = (Map<String, Object>) resultObj;
                points = (List<Map<String, Object>>) resultMap.get("points");
            } else {
                log.warn("Unexpected Qdrant search result type: {}", resultObj.getClass().getName());
                return Collections.emptyList();
            }

            if (points == null || points.isEmpty()) return Collections.emptyList();

            List<SearchResult> results = new ArrayList<>();
            for (Map<String, Object> point : points) {
                String id = String.valueOf(point.get("id"));
                Double score = point.get("score") instanceof Number n ? n.doubleValue() : 0.0;
                Map<String, Object> payload = (Map<String, Object>) point.getOrDefault("payload", Map.of());
                String content = payload.get("content") != null ? payload.get("content").toString() : "";

                results.add(SearchResult.builder()
                        .id(id)
                        .content(content)
                        .score(score)
                        .source(getStringFromPayload(payload, "source"))
                        .sourceId(getLongFromPayload(payload, "sourceId"))
                        .metadata(payload)
                        .build());
            }

            log.debug("Qdrant search returned {} results", results.size());
            return results;
        } catch (Exception e) {
            log.error("Failed to search Qdrant: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public void deleteDocument(String id) {
        try {
            String pointId = toQdrantId(id);
            Map<String, Object> request = new HashMap<>();
            request.put("points", List.of(pointId));

            webClient.post()
                    .uri("/collections/" + collectionName + "/points/delete")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            log.info("Deleted document from Qdrant: id={}", id);
        } catch (Exception e) {
            log.error("Failed to delete document from Qdrant: {}", e.getMessage());
        }
    }

    public Map<String, Object> getCollectionInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("collectionName", collectionName);
        try {
            Map<String, Object> response = webClient.get()
                    .uri("/collections/" + collectionName)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            if (response != null) {
                Map<String, Object> result = (Map<String, Object>) response.get("result");
                if (result != null) {
                    info.put("status", "healthy");
                    Map<String, Object> vectorCount = (Map<String, Object>) result.get("vectors_count");
                    info.put("vectorsCount", vectorCount);
                    info.put("pointsCount", result.get("points_count"));
                    info.put("state", result.get("state"));
                }
            }
        } catch (Exception e) {
            log.error("Failed to get Qdrant collection info: {}", e.getMessage());
            info.put("status", "error");
            info.put("error", e.getMessage());
        }
        return info;
    }

    public List<Map<String, Object>> getAllDocuments(int offset, int limit) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("limit", limit);
            request.put("offset", offset);
            request.put("with_payload", true);
            request.put("with_vector", false);

            Map<String, Object> response = webClient.post()
                    .uri("/collections/" + collectionName + "/points/scroll")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            if (response == null) return Collections.emptyList();

            Map<String, Object> result = (Map<String, Object>) response.get("result");
            if (result == null) return Collections.emptyList();

            List<Map<String, Object>> points = (List<Map<String, Object>>) result.get("points");
            if (points == null) return Collections.emptyList();

            List<Map<String, Object>> docs = new ArrayList<>();
            for (Map<String, Object> point : points) {
                Map<String, Object> doc = new LinkedHashMap<>();
                doc.put("id", String.valueOf(point.get("id")));
                Map<String, Object> payload = (Map<String, Object>) point.getOrDefault("payload", Map.of());
                doc.put("content", payload.get("content") != null ? payload.get("content").toString() : "");
                doc.put("source", payload.get("source"));
                doc.put("sourceId", payload.get("sourceId"));
                docs.add(doc);
            }
            return docs;
        } catch (Exception e) {
            log.error("Failed to scroll Qdrant documents: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Convert a string ID to a UUID-like format for Qdrant point ID.
     */
    private String toQdrantId(String id) {
        // Qdrant accepts UUID strings as point IDs
        try {
            UUID.fromString(id);
            return id;
        } catch (IllegalArgumentException e) {
            // Generate a deterministic UUID from the string
            return UUID.nameUUIDFromBytes(id.getBytes()).toString();
        }
    }

    private String getStringFromPayload(Map<String, Object> payload, String key) {
        Object val = payload.get(key);
        return val != null ? val.toString() : null;
    }

    private Long getLongFromPayload(Map<String, Object> payload, String key) {
        Object val = payload.get(key);
        if (val instanceof Long l) return l;
        if (val instanceof Number n) return n.longValue();
        if (val instanceof String s) {
            try { return Long.parseLong(s); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }
}
