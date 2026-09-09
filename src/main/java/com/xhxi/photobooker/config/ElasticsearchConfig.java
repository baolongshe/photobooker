package com.xhxi.photobooker.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "rag.elasticsearch.enabled", havingValue = "true", matchIfMissing = false)
public class ElasticsearchConfig {

    public ElasticsearchConfig() {
        log.info("Elasticsearch configuration initialized");
    }
}
