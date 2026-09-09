package com.xhxi.photobooker.agent;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class AgentToolRegistryTest {

    @Test
    void allToolFunctions_resolveToRegisteredBeans() {
        FakeTool searchPhotographer = new FakeTool("search_photographer");
        // SearchPortfolioTool.getName() 本身返回 camelCase，保持原样
        FakeTool searchPortfolio = new FakeTool("searchPortfolio");
        FakeTool searchPackages = new FakeTool("search_packages");
        FakeTool checkAvailability = new FakeTool("check_availability");
        FakeTool createOrder = new FakeTool("create_order");
        FakeTool updateMySelfInfo = new FakeTool("update_myself_info");
        FakeTool searchNearby = new FakeTool("search_nearby_photographer");
        // 已是 camelCase 的名称应保持原样
        FakeTool searchKnowledgeBase = new FakeTool("searchKnowledgeBase");

        AgentToolRegistry registry = new AgentToolRegistry(List.of(
                searchPhotographer, searchPortfolio, searchPackages,
                checkAvailability, createOrder, updateMySelfInfo,
                searchNearby, searchKnowledgeBase));

        Map<String, FakeTool> expected = new HashMap<>();
        expected.put("searchPhotographer", searchPhotographer);
        expected.put("searchPortfolio", searchPortfolio);
        expected.put("searchPackages", searchPackages);
        expected.put("checkAvailability", checkAvailability);
        expected.put("createOrder", createOrder);
        expected.put("updateMySelfInfo", updateMySelfInfo);
        expected.put("searchNearbyPhotographer", searchNearby);
        expected.put("searchKnowledgeBase", searchKnowledgeBase);

        // ALL_TOOL_FUNCTIONS 与 PUBLIC_TOOL_FUNCTIONS 中每个函数名都能解析到对应 bean
        for (String name : AgentToolRegistry.ALL_TOOL_FUNCTIONS) {
            assertNotNull(registry.resolve(name), "ALL_TOOL_FUNCTIONS 无法解析: " + name);
            assertSame(expected.get(name), registry.resolve(name), "ALL_TOOL_FUNCTIONS 解析错误: " + name);
        }
        for (String name : AgentToolRegistry.PUBLIC_TOOL_FUNCTIONS) {
            assertNotNull(registry.resolve(name), "PUBLIC_TOOL_FUNCTIONS 无法解析: " + name);
            assertSame(expected.get(name), registry.resolve(name), "PUBLIC_TOOL_FUNCTIONS 解析错误: " + name);
        }

        assertSame(searchKnowledgeBase, registry.resolve("searchKnowledgeBase"));
        assertNull(registry.resolve("unknownTool"));
    }

    @Test
    void emptyRegistry_returnsNullForAnyName() {
        AgentToolRegistry registry = new AgentToolRegistry(List.of());

        assertNull(registry.resolve("searchPhotographer"));
        assertNull(registry.resolve("createOrder"));
    }

    private static class FakeTool implements AgentTool {
        private final String name;

        FakeTool(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDescription() {
            return "fake";
        }

        @Override
        public Map<String, Object> getParameterSchema() {
            return Map.of();
        }

        @Override
        public Object execute(Map<String, Object> parameters) {
            return null;
        }
    }
}
