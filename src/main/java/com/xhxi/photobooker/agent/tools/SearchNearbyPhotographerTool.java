package com.xhxi.photobooker.agent.tools;

import com.xhxi.photobooker.agent.AgentTool;
import com.xhxi.photobooker.service.PhotographerService;
import com.xhxi.photobooker.vo.PhotographerNearbyVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 附近摄影师检索工具：经纬度+半径 → 距离升序列表（含距离km）。复用 Service 层，不直接碰 Redis */
@Component
public class SearchNearbyPhotographerTool implements AgentTool {

    @Autowired
    private PhotographerService photographerService;

    @Override
    public String getName() {
        return "search_nearby_photographer";
    }

    @Override
    public String getDescription() {
        return "按经纬度和半径检索附近摄影师，返回按距离升序的列表（含距离km）。当用户想找附近/离我近/周边的摄影师时使用";
    }

    @Override
    public Map<String, Object> getParameterSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("latitude", "number - 纬度（必填）");
        schema.put("longitude", "number - 经度（必填）");
        schema.put("radiusKm", "number - 检索半径（公里，默认5）");
        schema.put("limit", "number - 返回数量限制（默认10）");
        return schema;
    }

    @Override
    public Object execute(Map<String, Object> parameters) {
        try {
            Double latitude = parseDouble(parameters.get("latitude"));
            Double longitude = parseDouble(parameters.get("longitude"));
            Double radiusKm = parseDouble(parameters.get("radiusKm"));
            Integer limit = parameters.get("limit") != null
                    ? Integer.parseInt(parameters.get("limit").toString()) : null;

            // 校验范围与 /nearby 接口一致；越界返回错误文本，LLM 直接转述
            if (latitude == null || latitude < -90 || latitude > 90) {
                return "参数不合法: latitude 需在 [-90, 90]";
            }
            if (longitude == null || longitude < -180 || longitude > 180) {
                return "参数不合法: longitude 需在 [-180, 180]";
            }
            if (radiusKm == null) {
                radiusKm = 5.0;
            }
            if (limit == null) {
                limit = 10;
            }
            if (radiusKm <= 0 || radiusKm > 100) {
                return "参数不合法: radiusKm 需在 (0, 100] km";
            }
            if (limit < 1 || limit > 50) {
                return "参数不合法: limit 需在 [1, 50]";
            }

            // 注意：Service 签名纬度在前
            List<PhotographerNearbyVO> nearby = photographerService
                    .searchNearbyPhotographers(latitude, longitude, radiusKm, limit);
            return nearby.stream().map(p -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", p.getId());
                map.put("name", p.getName());
                map.put("phone", p.getPhone());
                map.put("avatar", p.getAvatar());
                map.put("working", p.getWorking());
                map.put("distanceKm", p.getDistanceKm());
                return map;
            }).collect(Collectors.toList());
        } catch (NumberFormatException e) {
            return "参数不合法: 数值格式错误";
        }
    }

    private Double parseDouble(Object value) {
        return value == null ? null : Double.parseDouble(value.toString());
    }
}
