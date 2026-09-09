package com.xhxi.photobooker.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.xhxi.photobooker.context.BaseContext;
import com.xhxi.photobooker.service.rag.RagPipelineService;
import com.xhxi.photobooker.utils.PromptInjectionGuard;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.concurrent.*;
import jakarta.annotation.PreDestroy;

/**
 * RAG chat controller providing SSE streaming and debug endpoints.
 */
@Slf4j
@RestController
@RequestMapping("/ai")
@ConditionalOnProperty(name = "rag.enabled", havingValue = "true")
public class RagChatController {

    @Autowired
    private RagPipelineService ragPipelineService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String RAG_HISTORY_KEY_PREFIX = "ai:rag:history:";
    private static final int MAX_HISTORY_SIZE = 10;
    private static final int RETAIN_RECENT_MESSAGES = 8;
    private static final long EMITTER_TIMEOUT_MS = 120_000L;

    private static final ExecutorService ragExecutor = new ThreadPoolExecutor(
            2, 8, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(50),
            new ThreadPoolExecutor.CallerRunsPolicy());

    /**
     * SSE streaming RAG chat endpoint.
     *
     * @param request JSON body with "question" and optional "chatHistory"
     * @return ResponseBodyEmitter for SSE streaming
     */
    @PostMapping("/rag-chat")
    public ResponseBodyEmitter ragChat(@RequestBody Map<String, String> request) {
        String question = request.get("question");
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("question is required");
        }
        if (question.length() > 4000) {
            throw new IllegalArgumentException("Message too long, max 4000 characters");
        }

        // Prompt injection check
        if (PromptInjectionGuard.isSevereInjection(question)) {
            ResponseBodyEmitter rejectEmitter = new ResponseBodyEmitter(EMITTER_TIMEOUT_MS);
            safeSend(rejectEmitter, PromptInjectionGuard.getInputRejectedResponse());
            safeComplete(rejectEmitter);
            return rejectEmitter;
        }
        String sanitizedQuestion = PromptInjectionGuard.sanitizeInput(question);

        ResponseBodyEmitter emitter = new ResponseBodyEmitter(EMITTER_TIMEOUT_MS);

        Long currentUserId = BaseContext.getCurrentId();
        log.info("RAG chat request from user: {}", currentUserId);

        ragExecutor.execute(() -> {
            try {
                if (currentUserId != null) {
                    BaseContext.setCurrentId(currentUserId);
                }

                log.info("========== RAG Request Start ==========");
                log.info("Question: {}", question);

                // Build chat history string from Redis
                String chatHistory = buildChatHistoryString(currentUserId);

                // Execute RAG pipeline
                Flux<String> stream = ragPipelineService.ragQuery(sanitizedQuestion, chatHistory);

                CountDownLatch latch = new CountDownLatch(1);
                StringBuilder fullResponse = new StringBuilder();

                stream.subscribe(
                        chunk -> {
                            safeSend(emitter, chunk);
                            fullResponse.append(chunk);
                        },
                        error -> {
                            log.error("RAG stream error", error);
                            safeCompleteError(emitter, error);
                            latch.countDown();
                        },
                        () -> {
                            try {
                                // Save chat history to Redis
                                if (currentUserId != null && fullResponse.length() > 0) {
                                    saveRagChatHistory(currentUserId, sanitizedQuestion, fullResponse.toString());
                                }
                                safeComplete(emitter);
                            } catch (Exception e) {
                                log.error("Failed to save RAG chat history", e);
                                safeComplete(emitter);
                            }
                            latch.countDown();
                        }
                );

                latch.await(110, TimeUnit.SECONDS);
                log.info("========== RAG Request End ==========");

            } catch (Exception e) {
                log.error("RAG chat request failed: {}", e.getMessage(), e);
                safeCompleteError(emitter, e);
            }
        });

        return emitter;
    }

    /**
     * Debug endpoint for admin testing.
     * Returns full pipeline results as JSON.
     *
     * @param request JSON body with "question" and optional "chatHistory"
     * @return debug information about each pipeline stage
     */
    @PostMapping("/rag-chat-debug")
    public Map<String, Object> ragChatDebug(@RequestBody Map<String, String> request) {
        String question = request.get("question");
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("question is required");
        }

        String chatHistory = request.get("chatHistory");
        log.info("RAG debug request: question=[{}]", question);

        return ragPipelineService.ragQueryDebug(question, chatHistory);
    }

    // ==================== Private Helpers ====================

    private String buildChatHistoryString(Long userId) {
        if (userId == null) return null;

        try {
            String redisKey = RAG_HISTORY_KEY_PREFIX + userId;
            String historyJson = redisTemplate.opsForValue().get(redisKey);
            if (historyJson != null && !historyJson.isBlank()) {
                List<Map<String, String>> history = JSON.parseObject(historyJson,
                        new TypeReference<List<Map<String, String>>>() {});

                // Build a readable chat history string
                StringBuilder sb = new StringBuilder();
                for (Map<String, String> msg : history) {
                    String role = msg.get("role");
                    String content = msg.get("content");
                    if ("user".equals(role)) {
                        sb.append("用户: ").append(content).append("\n");
                    } else if ("assistant".equals(role)) {
                        sb.append("助手: ").append(content).append("\n");
                    }
                }
                return sb.toString().trim();
            }
        } catch (Exception e) {
            log.warn("Failed to load RAG chat history for user {}: {}", userId, e.getMessage());
        }
        return null;
    }

    private void saveRagChatHistory(Long userId, String question, String answer) {
        try {
            String redisKey = RAG_HISTORY_KEY_PREFIX + userId;
            String historyJson = redisTemplate.opsForValue().get(redisKey);

            List<Map<String, String>> history;
            if (historyJson != null && !historyJson.isBlank()) {
                history = JSON.parseObject(historyJson,
                        new TypeReference<List<Map<String, String>>>() {});
            } else {
                history = new ArrayList<>();
            }

            // Add user message
            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", question);
            history.add(userMsg);

            // Add assistant response
            Map<String, String> assistantMsg = new HashMap<>();
            assistantMsg.put("role", "assistant");
            assistantMsg.put("content", answer);
            history.add(assistantMsg);

            // Trim history if too long
            List<Map<String, String>> toSave;
            if (history.size() > MAX_HISTORY_SIZE) {
                toSave = new ArrayList<>();
                toSave.addAll(history.subList(history.size() - RETAIN_RECENT_MESSAGES, history.size()));
            } else {
                toSave = new ArrayList<>(history);
            }

            redisTemplate.opsForValue().set(redisKey, JSON.toJSONString(toSave), 1, TimeUnit.HOURS);
            log.debug("RAG chat history saved for user {}", userId);
        } catch (Exception e) {
            log.error("Failed to save RAG chat history: {}", e.getMessage());
        }
    }

    private void safeSend(ResponseBodyEmitter emitter, Object data) {
        try {
            emitter.send(data);
        } catch (Exception e) {
            log.warn("Send to emitter failed: {}", e.getMessage());
        }
    }

    private void safeComplete(ResponseBodyEmitter emitter) {
        try {
            emitter.complete();
        } catch (Exception e) {
            log.warn("Complete emitter failed: {}", e.getMessage());
        }
    }

    private void safeCompleteError(ResponseBodyEmitter emitter, Throwable error) {
        try {
            emitter.completeWithError(error);
        } catch (Exception e) {
            log.warn("Complete emitter (error) failed: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down RAG chat thread pool...");
        ragExecutor.shutdown();
        try {
            if (!ragExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                ragExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            ragExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
