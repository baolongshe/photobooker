package com.xhxi.photobooker.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent 工具注册表。
 * <p>
 * 显式建立「Spring AI 模型可见函数名（camelCase，如 searchPhotographer）→
 * {@link AgentTool} Bean」的映射。现有 {@link AgentTool#getName()} 返回的是
 * snake_case（如 search_photographer），与模型看到的函数名（Spring AI 函数
 * Bean 名，如 searchPhotographer / updateMySelfInfo）不一致，因此必须显式映射，
 * 才能让 ReAct 循环把模型返回的 tool_calls 准确分发给底层工具实现。
 */
@Slf4j
@Component
public class AgentToolRegistry {

    /** 登录用户可用的全部工具函数名（与 Spring AI 注册的函数名一致） */
    public static final List<String> ALL_TOOL_FUNCTIONS = List.of(
            "searchPhotographer",
            "searchPortfolio",
            "searchPackages",
            "checkAvailability",
            "createOrder",
            "updateMySelfInfo",
            "searchNearbyPhotographer"
    );

    /** 未登录用户可用的公开工具函数名 */
    public static final List<String> PUBLIC_TOOL_FUNCTIONS = List.of(
            "searchPhotographer",
            "searchPackages",
            "checkAvailability",
            "searchPortfolio",
            "searchNearbyPhotographer"
    );

    /**
     * 显式映射：模型可见函数名（Spring AI 函数 Bean 名）-> AgentTool.getName()。
     * 注意 updateMySelfInfo 是 Bean 方法名，不能简单由 snake_case 推导。
     */
    private static final Map<String, String> MODEL_NAME_TO_TOOL_NAME = Map.ofEntries(
            Map.entry("searchPhotographer", "search_photographer"),
            Map.entry("searchPortfolio", "searchPortfolio"),
            Map.entry("searchPackages", "search_packages"),
            Map.entry("checkAvailability", "check_availability"),
            Map.entry("createOrder", "create_order"),
            Map.entry("updateMySelfInfo", "update_myself_info"),
            Map.entry("searchNearbyPhotographer", "search_nearby_photographer"),
            Map.entry("searchKnowledgeBase", "searchKnowledgeBase")
    );

    private final Map<String, AgentTool> toolsByModelName;

    public AgentToolRegistry(List<AgentTool> tools) {
        Map<String, AgentTool> map = new HashMap<>();
        if (tools != null) {
            for (AgentTool tool : tools) {
                for (Map.Entry<String, String> entry : MODEL_NAME_TO_TOOL_NAME.entrySet()) {
                    if (entry.getValue().equals(tool.getName())) {
                        AgentTool existing = map.put(entry.getKey(), tool);
                        if (existing != null) {
                            log.warn("Duplicate agent tool registration for {}: {} overrides {}",
                                    entry.getKey(), tool.getClass().getSimpleName(),
                                    existing.getClass().getSimpleName());
                        }
                        log.info("Registered agent tool: {} -> {}", entry.getKey(), tool.getClass().getSimpleName());
                    }
                }
            }
        }
        this.toolsByModelName = Collections.unmodifiableMap(map);
    }

    /**
     * 按模型可见函数名解析对应的工具 Bean。
     *
     * @param modelName 函数名（camelCase）
     * @return 对应的 {@link AgentTool}，未注册时返回 {@code null}
     */
    public AgentTool resolve(String modelName) {
        return toolsByModelName.get(modelName);
    }

    public List<String> getAllToolFunctions() {
        return ALL_TOOL_FUNCTIONS;
    }

    public List<String> getPublicToolFunctions() {
        return PUBLIC_TOOL_FUNCTIONS;
    }
}
