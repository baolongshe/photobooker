package com.xhxi.photobooker.agent.tools;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xhxi.photobooker.agent.AgentTool;
import com.xhxi.photobooker.entity.PhotographerPackage;
import com.xhxi.photobooker.mapper.PhotographerPackageMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class SearchPackagesTool implements AgentTool {

    @Autowired
    private PhotographerPackageMapper packageMapper;

    @Override
    public String getName() {
        return "search_packages";
    }

    @Override
    public String getDescription() {
        return "查询摄影套餐信息，支持按价格范围、摄影师筛选";
    }

    @Override
    public Map<String, Object> getParameterSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("photographerId", "number - 摄影师ID");
        schema.put("minPrice", "number - 最低价格");
        schema.put("maxPrice", "number - 最高价格");
        return schema;
    }

    @Override
    public Object execute(Map<String, Object> parameters) throws Exception {
        LambdaQueryWrapper<PhotographerPackage> wrapper = new LambdaQueryWrapper<>();

        if (parameters.containsKey("photographerId")) {
            Long photographerId = Long.parseLong(parameters.get("photographerId").toString());
            wrapper.eq(PhotographerPackage::getPhotographerId, photographerId);
        }

        if (parameters.containsKey("minPrice")) {
            BigDecimal minPrice = new BigDecimal(parameters.get("minPrice").toString());
            wrapper.ge(PhotographerPackage::getPrice, minPrice);
        }

        if (parameters.containsKey("maxPrice")) {
            BigDecimal maxPrice = new BigDecimal(parameters.get("maxPrice").toString());
            wrapper.le(PhotographerPackage::getPrice, maxPrice);
        }

        List<PhotographerPackage> packages = packageMapper.selectList(wrapper);

        return packages.stream().map(pkg -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", pkg.getId());
            map.put("name", pkg.getName());
            map.put("price", pkg.getPrice());
            map.put("serviceDetails", pkg.getServiceDetails());
            map.put("duration", pkg.getDuration());
            return map;
        }).collect(Collectors.toList());
    }
}
