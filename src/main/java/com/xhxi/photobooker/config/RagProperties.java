package com.xhxi.photobooker.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "rag")
public class RagProperties {

    private boolean enabled = true;

    private OllamaProperties ollama = new OllamaProperties();
    private QdrantProperties qdrant = new QdrantProperties();
    private ElasticsearchProperties elasticsearch = new ElasticsearchProperties();
    private RetrievalProperties retrieval = new RetrievalProperties();

    @Data
    public static class OllamaProperties {
        private String baseUrl = "http://localhost:11434";
        private String chatModel = "deepseek-r1:7b";
        private String embeddingModel = "nomic-embed-text";
    }

    @Data
    public static class QdrantProperties {
        private String host = "localhost";
        private int port = 6333;
        private String collectionName = "photobooker_knowledge";
        private int vectorSize = 1024;
    }

    @Data
    public static class ElasticsearchProperties {
        private boolean enabled = false;
        private String host = "localhost";
        private int port = 9200;
        private String indexName = "photobooker_knowledge";
    }

    @Data
    public static class RetrievalProperties {
        private int topK = 5;
        private double scoreThreshold = 0.7;
    }
}
