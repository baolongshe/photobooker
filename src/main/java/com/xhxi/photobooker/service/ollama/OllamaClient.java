package com.xhxi.photobooker.service.ollama;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.xhxi.photobooker.config.RagProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.*;

@Slf4j
@Service
@ConditionalOnProperty(name = "rag.enabled", havingValue = "true", matchIfMissing = false)
public class OllamaClient {

    private final WebClient webClient;
    private final RagProperties.OllamaProperties ollamaProps;

    public OllamaClient(RagProperties ragProperties) {
        this.ollamaProps = ragProperties.getOllama();
        this.webClient = WebClient.builder()
                .baseUrl(ollamaProps.getBaseUrl())
                .build();
        log.info("OllamaClient initialized with base URL: {}", ollamaProps.getBaseUrl());
    }

    /**
     * Generate embedding vector for the given text using Ollama embeddings API.
     *
     * @param text the text to embed
     * @return float array representing the embedding vector
     */
    public float[] generateEmbedding(String text) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", ollamaProps.getEmbeddingModel());
            requestBody.put("prompt", text);

            String response = webClient.post()
                    .uri("/api/embeddings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            if (response == null) {
                throw new RuntimeException("Empty response from Ollama embeddings API");
            }

            JSONObject json = JSON.parseObject(response);
            JSONArray embeddingArray = json.getJSONArray("embedding");

            if (embeddingArray == null || embeddingArray.isEmpty()) {
                throw new RuntimeException("No embedding data in Ollama response");
            }

            float[] embedding = new float[embeddingArray.size()];
            for (int i = 0; i < embeddingArray.size(); i++) {
                embedding[i] = embeddingArray.getFloatValue(i);
            }

            log.debug("Generated embedding with {} dimensions", embedding.length);
            return embedding;
        } catch (Exception e) {
            log.error("Failed to generate embedding from Ollama: {}", e.getMessage());
            throw new RuntimeException("Failed to generate embedding: " + e.getMessage(), e);
        }
    }

    /**
     * Non-streaming chat completion using Ollama chat API.
     *
     * @param systemPrompt system prompt
     * @param userMessage  user message
     * @return the assistant's response text
     */
    public String chatCompletion(String systemPrompt, String userMessage) {
        try {
            Map<String, Object> requestBody = buildChatRequest(systemPrompt, userMessage, false);

            String response = webClient.post()
                    .uri("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMinutes(2))
                    .block();

            if (response == null) {
                throw new RuntimeException("Empty response from Ollama chat API");
            }

            JSONObject json = JSON.parseObject(response);
            JSONObject message = json.getJSONObject("message");
            if (message != null) {
                return message.getString("content");
            }

            log.warn("Unexpected Ollama chat response format: {}", response);
            return "";
        } catch (Exception e) {
            log.error("Failed to get chat completion from Ollama: {}", e.getMessage());
            throw new RuntimeException("Failed to get chat completion: " + e.getMessage(), e);
        }
    }

    /**
     * Streaming chat completion using Ollama chat API.
     * Returns a Flux that emits content chunks as they arrive (NDJSON format).
     *
     * @param systemPrompt system prompt
     * @param userMessage  user message
     * @return Flux of content string chunks
     */
    public Flux<String> chatCompletionStream(String systemPrompt, String userMessage) {
        Map<String, Object> requestBody = buildChatRequest(systemPrompt, userMessage, true);

        return webClient.post()
                .uri("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(Duration.ofMinutes(5))
                .filter(line -> line != null && !line.isEmpty())
                .mapNotNull(line -> {
                    try {
                        JSONObject json = JSON.parseObject(line);
                        JSONObject message = json.getJSONObject("message");
                        if (message != null) {
                            String content = message.getString("content");
                            if (content != null && !content.isEmpty()) {
                                return content;
                            }
                        }
                    } catch (Exception e) {
                        log.debug("Failed to parse Ollama stream chunk: {}", line);
                    }
                    return null;
                })
                .doOnError(e -> log.error("Ollama stream error: {}", e.getMessage()))
                .doOnComplete(() -> log.debug("Ollama stream completed"));
    }

    private Map<String, Object> buildChatRequest(String systemPrompt, String userMessage, boolean stream) {
        List<Map<String, String>> messages = new ArrayList<>();

        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            Map<String, String> sysMsg = new HashMap<>();
            sysMsg.put("role", "system");
            sysMsg.put("content", systemPrompt);
            messages.add(sysMsg);
        }

        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", ollamaProps.getChatModel());
        requestBody.put("messages", messages);
        requestBody.put("stream", stream);

        return requestBody;
    }
}
