package com.xhxi.photobooker.config;

import com.xhxi.photobooker.agent.AgentTool;
import com.xhxi.photobooker.agent.FunctionCallContext;
import com.xhxi.photobooker.agent.tools.CheckAvailabilityTool;
import com.xhxi.photobooker.agent.tools.CreateOrderTool;
import com.xhxi.photobooker.agent.tools.SearchKnowledgeBaseTool;
import com.xhxi.photobooker.agent.tools.SearchNearbyPhotographerTool;
import com.xhxi.photobooker.agent.tools.SearchPackagesTool;
import com.xhxi.photobooker.agent.tools.SearchPhotographerTool;
import com.xhxi.photobooker.agent.tools.SearchPortfolioTool;
import com.xhxi.photobooker.agent.tools.UpdateMySelfInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Spring AI Function Calling 工具配置。
 * <p>
 * 将现有的 AgentTool 实现包装为 Spring AI 的 {@link Function} Bean。
 * Spring AI 自动将 Bean 名作为工具名、{@link Description} 作为工具描述、
 * 输入类型的字段作为工具参数生成 JSON Schema。
 * <p>
 * 使用方式：在 ChatClient 中通过 {@code .functions("searchPhotographer", "searchPackages", ...)} 引用。
 */
@Slf4j
@Configuration
public class AgentToolFunctionConfig {

    /**
     * 搜索摄影师。
     * Bean 名 "searchPhotographer" 通过 Spring AI 对应工具名 "searchPhotographer"，
     * 在 ChatClient 中用 .functions("searchPhotographer") 注册。
     */
    @Bean
    @Description("根据姓名、风格、评分、位置等条件搜索摄影师，返回摄影师列表（含姓名、评分、简介、电话等）")
    public Function<SearchPhotographerRequest, List<Map<String, Object>>> searchPhotographer(
            SearchPhotographerTool tool) {
        return request -> executeTool(tool, request.toParamMap());
    }

    /**
     * 搜索作品集。支持按标题、描述、标签、分类模糊匹配。
     */
    @Bean
    @Description("搜索摄影师作品集，支持按标题、描述、标签模糊匹配。当用户提到具体拍摄对象(角色名、宠物名)或风格时，优先使用此工具而非searchPhotographer")
    public Function<SearchPortfolioRequest, List<Map<String, Object>>> searchPortfolio(
            SearchPortfolioTool tool) {
        return request -> executeTool(tool, request.toParamMap());
    }

    /**
     * 查询套餐。
     */
    @Bean
    @Description("根据摄影师ID和价格范围查询摄影套餐，返回套餐列表（含名称、价格、服务详情、时长等）")
    public Function<SearchPackagesRequest, List<Map<String, Object>>> searchPackages(
            SearchPackagesTool tool) {
        return request -> executeTool(tool, request.toParamMap());
    }

    /**
     * 查找附近摄影师。基于 Redis GEO 距离检索，返回带距离的列表。
     */
    @Bean
    @Description("按经纬度和半径检索附近摄影师，返回按距离升序的列表（含距离km）。当用户想找附近/离我近/周边的摄影师时使用")
    public Function<SearchNearbyPhotographerRequest, List<Map<String, Object>>> searchNearbyPhotographer(
            SearchNearbyPhotographerTool tool) {
        return request -> executeTool(tool, request.toParamMap());
    }

    /**
     * 检查档期。
     */
    @Bean
    @Description("检查摄影师在指定时间是否可用")
    public Function<CheckAvailabilityRequest, Object> checkAvailability(
            CheckAvailabilityTool tool) {
        return request -> executeTool(tool, request.toParamMap());
    }

    /**
     * 创建订单。用户ID通过 {@link FunctionCallContext} 跨线程传递。
     */
    @Bean
    @Description("创建约拍订单，需要摄影师ID、套餐名称、价格、拍摄时间和地点，返回订单ID和支付信息")
    public Function<CreateOrderRequest, Object> createOrder(
            CreateOrderTool tool) {
        return request -> executeTool(tool, request.toParamMap(FunctionCallContext.getUserId()));
    }

    /**
     * 搜索知识库。当数据库精确查询无结果时，用语义搜索兜底。
     * 适用于角色名、风格描述、场景需求等模糊查询。
     */
    @Bean
    @ConditionalOnProperty(name = "rag.enabled", havingValue = "true") // 新增此注解
    @Description("在平台知识库中语义搜索，用于模糊查询...")
    public Function<SearchKnowledgeBaseRequest, List<Map<String, Object>>> searchKnowledgeBase(
            SearchKnowledgeBaseTool tool) {
        return request -> executeTool(tool, request.toParamMap());
    }


    /**
     * 更新个人信息。用户ID通过 {@link FunctionCallContext} 跨线程传递。
     */
    @Bean
    @Description("更新当前用户的个人信息，可修改真实姓名、联系电话、性别、生日、头像")
    public Function<UpdateMySelfInfoRequest, Object> updateMySelfInfo(
            UpdateMySelfInfo tool) {
        return request -> executeTool(tool, request.toParamMap(FunctionCallContext.getUserId()));
    }

    // ==================== 辅助方法 ====================

    @SuppressWarnings("unchecked")
    private <T> T executeTool(AgentTool tool, Map<String, Object> params) {
        try {
            log.info("执行工具: {}, 参数: {}", tool.getName(), params);
            Object result = tool.execute(params);
            return (T) result;
        } catch (Exception e) {
            log.error("工具执行失败: {} - {}", tool.getName(), e.getMessage(), e);
            throw new RuntimeException("工具执行失败: " + tool.getName() + " - " + e.getMessage());
        }
    }

    // ==================== 请求参数 POJO ====================
    // 这些内部类的字段被 Spring AI 自动转为 JSON Schema

    public record SearchPhotographerRequest(
            String name,
            String style,
            Double minRating,
            String location,
            Integer limit
    ) {
        Map<String, Object> toParamMap() {
            Map<String, Object> m = new HashMap<>();
            putIfNotNull(m, "name", name);
            putIfNotNull(m, "style", style);
            putIfNotNull(m, "minRating", minRating);
            putIfNotNull(m, "location", location);
            putIfNotNull(m, "limit", limit);
            return m;
        }
    }

    public record SearchPackagesRequest(
            Long photographerId,
            Double minPrice,
            Double maxPrice
    ) {
        Map<String, Object> toParamMap() {
            Map<String, Object> m = new HashMap<>();
            putIfNotNull(m, "photographerId", photographerId);
            putIfNotNull(m, "minPrice", minPrice);
            putIfNotNull(m, "maxPrice", maxPrice);
            return m;
        }
    }

    public record CheckAvailabilityRequest(
            Long photographerId,
            String requestedTime,
            Integer duration
    ) {
        Map<String, Object> toParamMap() {
            Map<String, Object> m = new HashMap<>();
            putIfNotNull(m, "photographerId", photographerId);
            putIfNotNull(m, "requestedTime", requestedTime);
            putIfNotNull(m, "duration", duration);
            return m;
        }
    }

    public record CreateOrderRequest(
            Long photographerId,
            String packageName,
            Double totalPrice,
            String shootingTime,
            String shootingLocation
    ) {
        Map<String, Object> toParamMap(Long userId) {
            Map<String, Object> m = new HashMap<>();
            putIfNotNull(m, "photographerId", photographerId);
            putIfNotNull(m, "packageName", packageName);
            m.put("totalPrice", totalPrice);
            putIfNotNull(m, "shootingTime", shootingTime);
            putIfNotNull(m, "shootingLocation", shootingLocation);
            if (userId != null) {
                m.put("userId", userId);
            }
            return m;
        }
    }

    public record SearchPortfolioRequest(
            String keyword,
            Long photographerId,
            String category,
            Integer limit
    ) {
        Map<String, Object> toParamMap() {
            Map<String, Object> m = new HashMap<>();
            putIfNotNull(m, "keyword", keyword);
            putIfNotNull(m, "photographerId", photographerId);
            putIfNotNull(m, "category", category);
            putIfNotNull(m, "limit", limit);
            return m;
        }
    }

    public record SearchNearbyPhotographerRequest(
            Double latitude,
            Double longitude,
            Double radiusKm,
            Integer limit
    ) {
        Map<String, Object> toParamMap() {
            Map<String, Object> m = new HashMap<>();
            putIfNotNull(m, "latitude", latitude);
            putIfNotNull(m, "longitude", longitude);
            putIfNotNull(m, "radiusKm", radiusKm);
            putIfNotNull(m, "limit", limit);
            return m;
        }
    }

    public record SearchKnowledgeBaseRequest(
            String query,
            Integer topK
    ) {
        Map<String, Object> toParamMap() {
            Map<String, Object> m = new HashMap<>();
            putIfNotNull(m, "query", query);
            putIfNotNull(m, "topK", topK);
            return m;
        }
    }

    public record UpdateMySelfInfoRequest(
            String realName,
            String phone,
            Integer gender,
            String birthday,
            String avatar
    ) {
        Map<String, Object> toParamMap(Long userId) {
            Map<String, Object> m = new HashMap<>();
            putIfNotNull(m, "realName", realName);
            putIfNotNull(m, "phone", phone);
            putIfNotNull(m, "gender", gender);
            putIfNotNull(m, "birthday", birthday);
            putIfNotNull(m, "avatar", avatar);
            if (userId != null) {
                m.put("userId", userId);
            }
            return m;
        }
    }

    private static void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }
}
