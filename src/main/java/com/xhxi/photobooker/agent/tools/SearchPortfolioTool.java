package com.xhxi.photobooker.agent.tools;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xhxi.photobooker.agent.AgentTool;
import com.xhxi.photobooker.entity.Portfolio;
import com.xhxi.photobooker.mapper.PortfolioMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Tool that searches portfolios by title, description, category, and tags.
 * Unlike the RAG knowledge base, this queries the database directly.
 */
@Slf4j
@Component
public class SearchPortfolioTool implements AgentTool {

    @Autowired
    private PortfolioMapper portfolioMapper;

    @Override
    public String getName() {
        return "searchPortfolio";
    }

    @Override
    public String getDescription() {
        return "搜索摄影师作品集，支持按标题、描述、分类、标签模糊匹配。"
                + "当用户提到具体拍摄对象（角色名、宠物名等）或作品风格时使用此工具";
    }

    @Override
    public Map<String, Object> getParameterSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("keyword", "string - 搜索关键词（会在标题、描述、标签、分类中模糊匹配）");
        schema.put("photographerId", "number - 限定摄影师ID（可选）");
        schema.put("category", "string - 作品分类过滤（婚纱/写真/纪实/商业/生活等，可选）");
        schema.put("limit", "number - 返回数量限制（默认10，最大20）");
        return schema;
    }

    @Override
    public Object execute(Map<String, Object> parameters) throws Exception {
        LambdaQueryWrapper<Portfolio> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Portfolio::getStatus, 1);

        // Build a combined LIKE condition across title, description, and tags
        if (parameters.containsKey("keyword")) {
            String keyword = (String) parameters.get("keyword");
            if (keyword != null && !keyword.isBlank()) {
                wrapper.and(w -> w
                        .like(Portfolio::getTitle, keyword)
                        .or()
                        .like(Portfolio::getDescription, keyword)
                        .or()
                        .like(Portfolio::getTags, keyword));
                log.info("Searching portfolios by keyword: {}", keyword);
            }
        }

        // Optional photographer filter
        if (parameters.containsKey("photographerId")) {
            Object pid = parameters.get("photographerId");
            if (pid instanceof Number) {
                wrapper.eq(Portfolio::getPhotographerId, ((Number) pid).longValue());
            }
        }

        // Optional category filter
        if (parameters.containsKey("category")) {
            String category = (String) parameters.get("category");
            if (category != null && !category.isBlank() && !"不限".equals(category)) {
                wrapper.eq(Portfolio::getCategory, category);
            }
        }

        int limit = 10;
        if (parameters.containsKey("limit")) {
            limit = Integer.parseInt(parameters.get("limit").toString());
        }
        limit = Math.max(1, Math.min(limit, 20));
        wrapper.orderByDesc(Portfolio::getSortWeight)
               .orderByDesc(Portfolio::getViewCount)
               .last("LIMIT " + limit);

        List<Portfolio> portfolios = portfolioMapper.selectList(wrapper);
        log.info("Found {} portfolios", portfolios.size());

        return portfolios.stream().map(p -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", p.getId());
            map.put("title", p.getTitle());
            map.put("photographerId", p.getPhotographerId());
            map.put("category", p.getCategory());
            map.put("tags", p.getTags());
            map.put("description", p.getDescription());
            map.put("shootingLocation", p.getShootingLocation());
            map.put("viewCount", p.getViewCount());
            map.put("likeCount", p.getLikeCount());
            return map;
        }).collect(java.util.stream.Collectors.toList());
    }
}
