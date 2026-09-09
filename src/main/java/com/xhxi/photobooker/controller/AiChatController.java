package com.xhxi.photobooker.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.xhxi.photobooker.agent.AgentSystemPrompt;
import com.xhxi.photobooker.agent.AgentToolRegistry;
import com.xhxi.photobooker.agent.FunctionCallContext;
import com.xhxi.photobooker.agent.react.ReActAgentService;
import com.xhxi.photobooker.context.BaseContext;
import com.xhxi.photobooker.service.ollama.OllamaClient;
import com.xhxi.photobooker.service.rag.RagPipelineService;
import com.xhxi.photobooker.utils.PromptInjectionGuard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import jakarta.annotation.PreDestroy;

@RestController
@RequestMapping("/ai")
public class AiChatController {
    private static final Logger logger = LoggerFactory.getLogger(AiChatController.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ReActAgentService reActAgentService;

    @Autowired
    private AgentToolRegistry agentToolRegistry;

    @Autowired(required = false)
    private OllamaClient ollamaClient;

    @Autowired(required = false)
    private RagPipelineService ragPipelineService;

    private static final String REDIS_KEY_PREFIX = "ai:chat:history:";
    private static final int MAX_HISTORY_SIZE = 10;
    private static final int RETAIN_RECENT_MESSAGES = 8;

    private static final Pattern DUPLICATE_GREETINGS_PATTERN = Pattern.compile("(您好，我是小影。?)+");
    private static final Pattern DUPLICATE_PUNCTUATION1_PATTERN = Pattern.compile("[。！？]{2,}");
    private static final Pattern DUPLICATE_PUNCTUATION2_PATTERN = Pattern.compile("[，、]{2,}");
    private static final Pattern EXCESSIVE_SPACES_PATTERN = Pattern.compile("\\s+");

    private static final ExecutorService chatExecutor = new ThreadPoolExecutor(
            2, 10, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(50),
            new ThreadPoolExecutor.CallerRunsPolicy());

    /**
     * 查询当前会话的任务状态
     */
    @GetMapping("/task/status")
    public Map<String, Object> getTaskStatus(@RequestParam("sessionId") String sessionId) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("task", null);
        result.put("progress", 0);
        result.put("currentStepName", "");
        return result;
    }

    @PostMapping("/stream-chat")
    public ResponseBodyEmitter streamChat(@RequestParam("sessionId") String sessionId,
                                          @RequestBody String message) {
        if (message != null && message.length() > 4000) {
            throw new IllegalArgumentException("Message too long, max 4000 characters");
        }

        // ReAct 多轮 LLM 调用耗时长，超时从 60s 提高到 120s
        ResponseBodyEmitter emitter = new ResponseBodyEmitter(120000L);

        Long currentUserId = BaseContext.getCurrentId();
        logger.info("主线程获取用户ID: {}", currentUserId);

        chatExecutor.execute(() -> {
            try {
                if (currentUserId != null) {
                    BaseContext.setCurrentId(currentUserId);
                }

                logger.info("========== AI Request Start ==========");
                logger.info("User message: {}", message);
                logger.info("Normal chat mode - Spring AI Agent mode");

                processNormalChat(emitter, sessionId, message, currentUserId);
                logger.info("========== AI Request End ==========");
            } catch (Exception e) {
                logger.error("AI chat request failed: {}", e.getMessage(), e);
                safeCompleteError(emitter, e);
            }
        });

        return emitter;
    }

    // ==================== Core Business Methods ====================

    private void processNormalChat(ResponseBodyEmitter emitter, String sessionId,
                                   String message, Long currentUserId) throws Exception {

        // ========== 第1层防护：输入侧清洗 ==========
        // 检测严重注入攻击，直接拒绝
        if (PromptInjectionGuard.isSevereInjection(message)) {
            logger.warn("检测到严重提示词注入攻击，拒绝处理");
            safeSend(emitter, PromptInjectionGuard.getInputRejectedResponse());
            safeComplete(emitter);
            return;
        }
        // 清洗用户输入
        String sanitizedMessage = PromptInjectionGuard.sanitizeInput(message);
        logger.info("用户输入已清洗");

        String redisKey = REDIS_KEY_PREFIX + sessionId;
        String historyJson = (String) redisTemplate.opsForValue().get(redisKey);
        List<Map<String, String>> history = getOrInitChatHistory(historyJson);

        String systemPrompt = AgentSystemPrompt.build(currentUserId);

        // ========== 第2层防护：XML标签隔离用户输入 ==========
        addUserMessageToHistory(history, sanitizedMessage);

        List<String> toolFunctions = currentUserId != null
                ? agentToolRegistry.getAllToolFunctions()
                : agentToolRegistry.getPublicToolFunctions();
        if (currentUserId == null) {
            logger.info("用户未登录，仅注册公开工具");
        }

        if (currentUserId != null) {
            FunctionCallContext.setUserId(currentUserId);
        }

        // ========== ReAct 工具调用循环 ==========
        String finalAnswer;
        try {
            finalAnswer = reActAgentService.chat(systemPrompt, history, toolFunctions);
        } catch (Exception e) {
            // ModelScope DeepSeek rate-limited/unavailable -> fall back to RAG pipeline (knowledge base),
            // then to bare Ollama if RAG is not available either
            logger.warn("ReAct agent failed ({}), falling back to RAG/Ollama", e.getMessage(), e);
            finalAnswer = fallbackChat(systemPrompt, sanitizedMessage);
        }

        if (finalAnswer == null || finalAnswer.isBlank()) {
            logger.warn("LLM返回空响应，返回安全话术");
            finalAnswer = PromptInjectionGuard.getSafeFallbackResponse();
        }

        // ========== 第3层防护：输出侧拒答检测 ==========
        if (!PromptInjectionGuard.isOutputSafe(finalAnswer)) {
            logger.warn("LLM输出检测到异常，丢弃响应并返回安全话术");
            safeSend(emitter, PromptInjectionGuard.getSafeFallbackResponse());
            safeComplete(emitter);
            return;
        }

        String processedResponse = postProcessResponse(finalAnswer);
        saveChatHistory(history, processedResponse, redisKey);

        // ReAct 循环完成后，把最终答案作为整体 chunk 发送（纯文本 SSE 协议不变）
        safeSend(emitter, processedResponse);
        safeComplete(emitter);
    }

    /**
     * ReAct 主链路失败时的兜底：RAG 流水线 -> Ollama。
     */
    private String fallbackChat(String systemPrompt, String sanitizedMessage) {
        if (ragPipelineService != null) {
            try {
                List<String> chunks = ragPipelineService.ragQuery(sanitizedMessage, null)
                        .collectList()
                        .block(Duration.ofSeconds(100));
                if (chunks != null && !chunks.isEmpty()) {
                    return String.join("", chunks);
                }
            } catch (Exception e) {
                logger.warn("RAG fallback failed ({}), trying Ollama", e.getMessage());
            }
        }
        if (ollamaClient != null) {
            try {
                return ollamaClient.chatCompletion(systemPrompt, sanitizedMessage);
            } catch (Exception e) {
                logger.warn("Ollama fallback failed: {}", e.getMessage());
            }
        }
        return null;
    }

    // ==================== Chat History Management ====================

    private void saveChatHistory(List<Map<String, String>> history,
                                 String processedResponse, String redisKey) {
        Map<String, String> aiMsg = new HashMap<>();
        aiMsg.put("role", "assistant");
        aiMsg.put("content", processedResponse);
        history.add(aiMsg);

        List<Map<String, String>> toSave;
        if (history.size() > MAX_HISTORY_SIZE) {
            toSave = new ArrayList<>();
            toSave.add(history.get(0));
            toSave.addAll(history.subList(history.size() - RETAIN_RECENT_MESSAGES, history.size()));
        } else {
            toSave = new ArrayList<>(history);
        }

        redisTemplate.opsForValue().set(redisKey, JSON.toJSONString(toSave), 1, TimeUnit.HOURS);
        logger.info("Chat history saved to Redis");
    }

    private List<Map<String, String>> getOrInitChatHistory(String historyJson) {
        if (historyJson != null) {
            return JSON.parseObject(historyJson, new TypeReference<List<Map<String, String>>>() {});
        }

        List<Map<String, String>> history = new ArrayList<>();
        Map<String, String> sysMsg = new HashMap<>();
        sysMsg.put("role", "system");
        sysMsg.put("content", "你是小影，摄影约拍平台的客服助手。请遵循以下原则：\n" +
                "1. 保持回答的一致性，不要前后矛盾\n" +
                "2. 提供具体、准确的信息\n" +
                "3. 回答要简洁明了，避免重复\n" +
                "4. 如果信息不确定，请说明需要进一步确认\n" +
                "5. 始终保持友好和专业的服务态度");
        history.add(sysMsg);
        return history;
    }

    private void addUserMessageToHistory(List<Map<String, String>> history, String message) {
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        // 用 XML 标签包裹用户输入，实现与系统指令的硬隔离
        userMsg.put("content", PromptInjectionGuard.wrapUserInput(message));
        history.add(userMsg);
    }

    // ==================== Post Processing ====================

    private String postProcessResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return "抱歉，我现在无法回答您的问题，请稍后再试。";
        }

        response = DUPLICATE_GREETINGS_PATTERN.matcher(response).replaceAll("您好，我是小影。");
        response = DUPLICATE_PUNCTUATION1_PATTERN.matcher(response).replaceAll("。");
        response = DUPLICATE_PUNCTUATION2_PATTERN.matcher(response).replaceAll("，");
        response = EXCESSIVE_SPACES_PATTERN.matcher(response).replaceAll(" ");

        return response.trim();
    }

    // ==================== Emitter Helper Methods ====================

    private void safeSend(ResponseBodyEmitter emitter, Object data) {
        try {
            emitter.send(data);
        } catch (Exception e) {
            logger.warn("Send to emitter failed: {}", e.getMessage());
        }
    }

    private void safeComplete(ResponseBodyEmitter emitter) {
        try {
            emitter.complete();
        } catch (Exception e) {
            logger.warn("Complete emitter failed: {}", e.getMessage());
        }
    }

    private void safeCompleteError(ResponseBodyEmitter emitter, Throwable error) {
        try {
            emitter.completeWithError(error);
        } catch (Exception e) {
            logger.warn("Complete emitter (error) failed: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down AI chat thread pool...");
        chatExecutor.shutdown();
        try {
            if (!chatExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                chatExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            chatExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
