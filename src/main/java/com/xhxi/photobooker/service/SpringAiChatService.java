package com.xhxi.photobooker.service;

import com.xhxi.photobooker.utils.PromptInjectionGuard;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Spring AI 驱动的聊天服务（适配 Spring AI 1.0.0-M6 API）。
 * <p>
 * 使用 Spring AI 的 ChatClient + OpenAiChatOptions
 * 实现流式对话和原生 Function Calling。
 */
@Slf4j
@Service
public class SpringAiChatService {

    private final ChatClient chatClient;

    public SpringAiChatService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public Flux<String> streamChat(String systemPrompt, String userMessage) {
        log.info("SpringAiChatService.streamChat()");

        // 用 XML 标签包裹用户输入，防止提示词注入
        String wrappedMessage = PromptInjectionGuard.wrapUserInput(userMessage);

        return chatClient.prompt()
                .system(systemPrompt)
                .user(wrappedMessage)
                .stream()
                .content();
    }

    public Flux<String> streamChatWithTools(
            String systemPrompt,
            String userMessage,
            String... toolFunctionNames) {

        log.info("SpringAiChatService.streamChatWithTools()");

        // 用 XML 标签包裹用户输入
        String wrappedMessage = PromptInjectionGuard.wrapUserInput(userMessage);

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .functions(Set.of(toolFunctionNames))
                .build();

        return chatClient.prompt()
                .system(systemPrompt)
                .user(wrappedMessage)
                .options(options)
                .stream()
                .content();
    }

    public Flux<String> streamChatWithHistory(
            String systemPrompt,
            List<Map<String, String>> history,
            String... toolFunctionNames) {

        log.info("SpringAiChatService.streamChatWithHistory()");

        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));

        for (Map<String, String> msg : history) {
            String role = msg.get("role");
            String content = msg.get("content");
            if (content == null || content.isEmpty()) continue;

            switch (role) {
                case "user" -> messages.add(new UserMessage(content));
                case "assistant" -> messages.add(new AssistantMessage(content));
                case "system" -> { /* skip, systemPrompt above replaces it */ }
                default -> log.warn("未知消息角色: {}", role);
            }
        }

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .functions(Set.of(toolFunctionNames))
                .build();

        return chatClient.prompt()
                .messages(messages)
                .options(options)
                .stream()
                .content();
    }


    public Flux<String> agentChat(String systemPrompt, String userMessage, String... toolFunctionNames) {
        log.info("SpringAiChatService.agentChat()");

        // 用 XML 标签包裹用户输入
        String wrappedMessage = PromptInjectionGuard.wrapUserInput(userMessage);

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .functions(Set.of(toolFunctionNames))
                .build();

        return chatClient.prompt()
                .system(systemPrompt)
                .user(wrappedMessage)
                .options(options)
                .stream()
                .content();
    }
}
