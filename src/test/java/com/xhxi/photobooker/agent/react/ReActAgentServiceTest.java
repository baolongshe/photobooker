package com.xhxi.photobooker.agent.react;

import com.xhxi.photobooker.agent.AgentTool;
import com.xhxi.photobooker.agent.AgentToolRegistry;
import com.xhxi.photobooker.config.ReActProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReActAgentServiceTest {

    @Mock
    private ChatModel chatModel;

    @Mock
    private AgentToolRegistry toolRegistry;

    private ReActProperties properties;
    private ReActAgentService service;

    @BeforeEach
    void setUp() {
        properties = new ReActProperties();
        service = new ReActAgentService(chatModel, toolRegistry, properties);
    }

    @Test
    void chat_noToolCalls_returnsFinalAnswerDirectly() {
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(textResponse("您好，我是小影，请问有什么可以帮您？"));

        String result = service.chat("system", history(), AgentToolRegistry.ALL_TOOL_FUNCTIONS);

        assertEquals("您好，我是小影，请问有什么可以帮您？", result);
        verify(chatModel, times(1)).call(any(Prompt.class));
        verify(toolRegistry, times(0)).resolve(anyString());
    }

    @Test
    void chat_singleToolCall_executesAndBackfillsResult() {
        RecordingTool searchTool = new RecordingTool("searchPhotographer", Map.of("found", true, "name", "张三"), new ArrayList<>());
        when(toolRegistry.resolve("searchPhotographer")).thenReturn(searchTool);
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(
                        toolCallResponse("call_1", "searchPhotographer", "{\"name\":\"张三\"}"),
                        textResponse("已为您找到摄影师张三。"));

        String result = service.chat("system", history(), AgentToolRegistry.ALL_TOOL_FUNCTIONS);

        assertEquals("已为您找到摄影师张三。", result);
        assertEquals(List.of("searchPhotographer"), searchTool.getCalls());

        ArgumentCaptor<Prompt> captor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel, times(2)).call(captor.capture());

        // 第二轮 Prompt 必须包含带 tool_calls 的 AssistantMessage 与回填的 ToolResponseMessage
        List<Message> secondMessages = captor.getAllValues().get(1).getInstructions();
        AssistantMessage assistantWithCalls = secondMessages.stream()
                .filter(m -> m instanceof AssistantMessage a && a.hasToolCalls())
                .map(m -> (AssistantMessage) m)
                .findFirst()
                .orElseThrow(() -> new AssertionError("缺少带 tool_calls 的 AssistantMessage"));
        assertEquals("searchPhotographer", assistantWithCalls.getToolCalls().get(0).name());

        ToolResponseMessage toolResponse = secondMessages.stream()
                .filter(m -> m instanceof ToolResponseMessage)
                .map(m -> (ToolResponseMessage) m)
                .findFirst()
                .orElseThrow(() -> new AssertionError("缺少 ToolResponseMessage"));
        assertEquals(1, toolResponse.getResponses().size());
        assertEquals("searchPhotographer", toolResponse.getResponses().get(0).name());
        assertTrue(toolResponse.getResponses().get(0).responseData().contains("张三"));
        assertTrue(toolResponse.getResponses().get(0).responseData().contains("<tool_result"));
    }

    @Test
    void chat_multiStepChain_executesToolsInOrder() {
        List<String> calls = new ArrayList<>();
        RecordingTool photographerTool = new RecordingTool("searchPhotographer", List.of(Map.of("id", 1)), calls);
        RecordingTool packagesTool = new RecordingTool("searchPackages", List.of(Map.of("id", 11)), calls);
        RecordingTool availabilityTool = new RecordingTool("checkAvailability", Map.of("available", true), calls);
        RecordingTool createOrderTool = new RecordingTool("createOrder", Map.of("orderId", 99), calls);

        when(toolRegistry.resolve(anyString())).thenAnswer(inv -> {
            String name = inv.getArgument(0);
            return switch (name) {
                case "searchPhotographer" -> photographerTool;
                case "searchPackages" -> packagesTool;
                case "checkAvailability" -> availabilityTool;
                case "createOrder" -> createOrderTool;
                default -> null;
            };
        });
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(
                        toolCallResponse("c1", "searchPhotographer", "{\"name\":\"张三\"}"),
                        toolCallResponse("c2", "searchPackages", "{\"photographerId\":1}"),
                        toolCallResponse("c3", "checkAvailability", "{\"photographerId\":1,\"requestedTime\":\"2026-08-20 14:00\"}"),
                        toolCallResponse("c4", "createOrder", "{\"photographerId\":1,\"packageName\":\"A\",\"totalPrice\":299,\"shootingTime\":\"2026-08-20 14:00\",\"shootingLocation\":\"广州\"}"),
                        textResponse("订单已创建，订单号 99。"));

        String result = service.chat("system", history(), AgentToolRegistry.ALL_TOOL_FUNCTIONS);

        assertEquals("订单已创建，订单号 99。", result);
        assertEquals(List.of("searchPhotographer", "searchPackages", "checkAvailability", "createOrder"), calls);
        verify(chatModel, times(5)).call(any(Prompt.class));
    }

    @Test
    void chat_toolThrowsException_errorJsonBackfilledAndLoopContinues() {
        AgentTool failingTool = new RecordingTool("searchPhotographer", new RuntimeException("数据库连接失败"), new ArrayList<>());
        when(toolRegistry.resolve("searchPhotographer")).thenReturn(failingTool);
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(
                        toolCallResponse("call_1", "searchPhotographer", "{\"name\":\"张三\"}"),
                        textResponse("抱歉，暂时无法查询摄影师信息。"));

        String result = service.chat("system", history(), AgentToolRegistry.ALL_TOOL_FUNCTIONS);

        assertEquals("抱歉，暂时无法查询摄影师信息。", result);

        ArgumentCaptor<Prompt> captor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel, times(2)).call(captor.capture());
        List<Message> secondMessages = captor.getAllValues().get(1).getInstructions();
        ToolResponseMessage toolResponse = secondMessages.stream()
                .filter(m -> m instanceof ToolResponseMessage)
                .map(m -> (ToolResponseMessage) m)
                .findFirst()
                .orElseThrow(() -> new AssertionError("缺少 ToolResponseMessage"));
        assertTrue(toolResponse.getResponses().get(0).responseData().contains("数据库连接失败"));
    }

    @Test
    void chat_sameToolSameArgsRepeated_detectsStuckAndStopsEarly() {
        RecordingTool searchTool = new RecordingTool("searchPhotographer", List.of(Map.of("id", 1)), new ArrayList<>());
        when(toolRegistry.resolve("searchPhotographer")).thenReturn(searchTool);
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(
                        toolCallResponse("call_1", "searchPhotographer", "{\"name\":\"张三\"}"),
                        toolCallResponse("call_2", "searchPhotographer", "{\"name\":\"张三\"}"));

        String result = service.chat("system", history(), AgentToolRegistry.ALL_TOOL_FUNCTIONS);

        assertTrue(result.contains("无法完成"));
        verify(chatModel, times(2)).call(any(Prompt.class));
        // 第二轮重复调用在检测到卡死后不再执行工具
        assertEquals(1, searchTool.getCalls().size());
    }

    @Test
    void chat_sameFailingCallRetriesOnceThenAborts() {
        AgentTool failingTool = new RecordingTool("searchPhotographer", new RuntimeException("数据库连接失败"), new ArrayList<>());
        when(toolRegistry.resolve("searchPhotographer")).thenReturn(failingTool);
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(
                        toolCallResponse("call_1", "searchPhotographer", "{\"name\":\"张三\"}"),
                        toolCallResponse("call_2", "searchPhotographer", "{\"name\":\"张三\"}"),
                        toolCallResponse("call_3", "searchPhotographer", "{\"name\":\"张三\"}"));

        String result = service.chat("system", history(), AgentToolRegistry.ALL_TOOL_FUNCTIONS);

        assertTrue(result.contains("无法完成"));
        verify(chatModel, times(3)).call(any(Prompt.class));
        // 第一次失败 + 允许的一次重试，第三次请求被拦截
        verify(toolRegistry, times(2)).resolve("searchPhotographer");
    }

    @Test
    void chat_exceedsMaxIterations_degradesGracefully() {
        properties.setMaxIterations(3);
        RecordingTool searchTool = new RecordingTool("searchPhotographer", List.of(Map.of("id", 1)), new ArrayList<>());
        when(toolRegistry.resolve("searchPhotographer")).thenReturn(searchTool);
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(
                        toolCallResponse("call_1", "searchPhotographer", "{\"name\":\"a\"}"),
                        toolCallResponse("call_2", "searchPhotographer", "{\"name\":\"b\"}"),
                        toolCallResponse("call_3", "searchPhotographer", "{\"name\":\"c\"}"));

        String result = service.chat("system", history(), AgentToolRegistry.ALL_TOOL_FUNCTIONS);

        assertTrue(result.contains("抱歉"));
        verify(chatModel, times(3)).call(any(Prompt.class));
        assertEquals(3, searchTool.getCalls().size());
    }

    @Test
    void chat_disabled_returnsFallbackMessageImmediately() {
        properties.setEnabled(false);

        String result = service.chat("system", history(), AgentToolRegistry.ALL_TOOL_FUNCTIONS);

        assertTrue(result.contains("暂不可用"));
        verify(chatModel, times(0)).call(any(Prompt.class));
    }

    // ==================== 测试辅助 ====================

    private List<Map<String, String>> history() {
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", "<user_input>\n你好\n</user_input>");
        return List.of(userMsg);
    }

    private ChatResponse toolCallResponse(String id, String name, String arguments) {
        AssistantMessage message = new AssistantMessage("", Map.of(),
                List.of(new AssistantMessage.ToolCall(id, "function", name, arguments)));
        return new ChatResponse(List.of(new Generation(message)));
    }

    private ChatResponse textResponse(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }

    /**
     * 记录调用的假工具；result 为 Object 时返回，为 RuntimeException 时抛出。
     */
    private static class RecordingTool implements AgentTool {
        private final String name;
        private final Object result;
        private final List<String> calls;

        RecordingTool(String name, Object result, List<String> calls) {
            this.name = name;
            this.result = result;
            this.calls = calls;
        }

        List<String> getCalls() {
            return calls;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDescription() {
            return "fake tool " + name;
        }

        @Override
        public Map<String, Object> getParameterSchema() {
            return Map.of();
        }

        @Override
        public Object execute(Map<String, Object> parameters) throws Exception {
            calls.add(name);
            if (result instanceof RuntimeException e) {
                throw e;
            }
            return result;
        }
    }
}
