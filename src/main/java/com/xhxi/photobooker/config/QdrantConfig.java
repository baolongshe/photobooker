package com.xhxi.photobooker.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "rag.enabled", havingValue = "true", matchIfMissing = false)
public class QdrantConfig {

    /**
     * Qdrant REST client using WebClient.
     * Avoids gRPC/protobuf dependency conflicts entirely.
     */
    public QdrantConfig(RagProperties ragProperties, WebClient.Builder webClientBuilder) {
        log.info("Qdrant REST client configuration initialized for {}:{}",
                ragProperties.getQdrant().getHost(), ragProperties.getQdrant().getPort());
    }
}
