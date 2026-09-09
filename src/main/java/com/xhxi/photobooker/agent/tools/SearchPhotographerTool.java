package com.xhxi.photobooker.agent.tools;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xhxi.photobooker.agent.AgentTool;
import com.xhxi.photobooker.entity.Photographer;
import com.xhxi.photobooker.mapper.PhotographerMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@Slf4j
@Component
public class SearchPhotographerTool implements AgentTool {

    @Autowired
    private PhotographerMapper photographerMapper;

    @Override
    public String getName() {
        return "search_photographer";
    }

    @Override
    public String getDescription() {
        return "根据条件搜索摄影师，支持按风格、评分、位置筛选";
    }

    @Override
    public Map<String, Object> getParameterSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("name", "string - 摄影师姓名（精确匹配）");
        schema.put("style", "string - 拍摄风格（婚纱/写真/纪实等）");
        schema.put("minRating", "number - 最低评分");
        schema.put("location", "string - 地理位置");
        schema.put("limit", "number - 返回数量限制（默认10）");
        return schema;
    }

    @Override
    public Object   execute(Map<String, Object> parameters) throws Exception {
        LambdaQueryWrapper<Photographer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Photographer::getAuthStatus, 1);

        // 优先按姓名精确匹配
        if (parameters.containsKey("name")) {
            String name = (String) parameters.get("name");
            if (name != null && !name.isBlank()) {
                wrapper.like(Photographer::getName, name);
                log.info("按摄影师姓名搜索: {}", name);
            }
        }

        if (parameters.containsKey("style")) {
            String style = (String) parameters.get("style");
            if (style != null && !style.isBlank()
                    && !"不限".equals(style) && !"all".equalsIgnoreCase(style)) {
                wrapper.like(Photographer::getStyle, style);
            }
        }

        if (parameters.containsKey("minRating")) {
            Double minRating = Double.parseDouble(parameters.get("minRating").toString());
            wrapper.ge(Photographer::getRating, minRating);
        }

        if (parameters.containsKey("location")) {
            String location = (String) parameters.get("location");
            if (location != null && !location.isBlank()) {
                wrapper.like(Photographer::getLocation, location);
            }
        }

        int limit = parameters.containsKey("limit") ?
            Integer.parseInt(parameters.get("limit").toString()) : 10;
        limit = Math.max(1, Math.min(limit, 50));
        wrapper.orderByDesc(Photographer::getRating).last("LIMIT " + limit);

        List<Photographer> photographers = photographerMapper.selectList(wrapper);

       log.info("搜索到 {} 个摄影师", photographers.size());

        return photographers.stream().map(p -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", p.getId());
            map.put("name", p.getName());
            map.put("style", p.getStyle());
            map.put("rating", p.getRating());
            map.put("orderCount", p.getOrderCount());
            map.put("intro", p.getIntro());
            map.put("phone", p.getPhone());
            return map;
        }).collect(Collectors.toList());
    }
}
