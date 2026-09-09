package com.xhxi.photobooker.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xhxi.photobooker.entity.Photographer;
import com.xhxi.photobooker.entity.PhotographerPackage;
import com.xhxi.photobooker.mapper.PhotographerMapper;
import com.xhxi.photobooker.mapper.PhotographerPackageMapper;
import com.xhxi.photobooker.service.GeoDistanceResult;
import com.xhxi.photobooker.service.OrderService;
import com.xhxi.photobooker.service.PhotographerGeoService;
import com.xhxi.photobooker.service.PhotographerPackageService;
import com.xhxi.photobooker.service.PhotographerService;
import com.xhxi.photobooker.vo.PhotographerNearbyVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.redis.core.RedisTemplate;
import java.util.concurrent.TimeUnit;

@Service
public class PhotographerServiceImpl implements PhotographerService {

    private static final Logger log = LoggerFactory.getLogger(PhotographerServiceImpl.class);

    @Autowired
    private PhotographerMapper photographerMapper;
    
    @Autowired
    private OrderService orderService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private PhotographerGeoService photographerGeoService;

    @Autowired
    private PhotographerPackageService photographerPackageService;

    @Override
    public Photographer savePhotographer(Photographer photographer) {
        photographerMapper.insert(photographer);
        return photographer;
    }

    @Override
    public Photographer findPhotographerById(Long id) {
        String cacheKey = "photographer:detail:" + id;
        Photographer photographer = (Photographer) redisTemplate.opsForValue().get(cacheKey);
        if (photographer != null) {
            return photographer;
        }
        photographer = photographerMapper.selectById(id);
        if (photographer != null) {
            // 动态计算完成订单数
            Integer completedCount = orderService.getCompletedOrderCount(id);
            photographer.setOrderCount(completedCount != null ? completedCount : 0);
            // 写入缓存，设置过期时间1小时
            redisTemplate.opsForValue().set(cacheKey, photographer, 1, TimeUnit.HOURS);
        }
        return photographer;
    }

    @Override
    public List<Photographer> findAllPhotographer() {
        List<Photographer> photographers = photographerMapper.selectList(null);
        // 为每个摄影师动态计算完成订单数
        for (Photographer photographer : photographers) {
            Integer completedCount = orderService.getCompletedOrderCount(photographer.getId());
            photographer.setOrderCount(completedCount != null ? completedCount : 0);
        }
        return photographers;
    }

    @Override
    public Photographer updatePhotographer(Long id, Photographer photographer) {
        Photographer photographer1 = photographerMapper.selectByUserId(id);
        if (photographer1 != null) {
            photographerMapper.updateById(photographer);
            // GEO 双写：与 MyBatis-Plus NOT_NULL 更新语义对齐——body 未传的坐标取 DB 现值合并后决策
            if (photographer.getId() != null) {
                Double lat = photographer.getLatitude() != null ? photographer.getLatitude() : photographer1.getLatitude();
                Double lng = photographer.getLongitude() != null ? photographer.getLongitude() : photographer1.getLongitude();
                if (lat != null && lng != null) {
                    photographerGeoService.add(photographer.getId(), lng, lat);
                } else {
                    photographerGeoService.remove(photographer.getId());
                }
            }
        }
        return null;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean deletePhotographer(Long id) {
        // 先删除该摄影师的所有套餐
        QueryWrapper<PhotographerPackage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("photographer_id", id);
        photographerPackageService.remove(queryWrapper);

        // 再删除摄影师
        int result = photographerMapper.deleteById(id);
        if (result > 0) {
            // 同步从 GEO 索引移除；Redis 故障不影响主流程，由启动全量同步兜底
            try {
                photographerGeoService.remove(id);
            } catch (Exception e) {
                log.warn("从 GEO 索引移除摄影师 {} 失败: {}", id, e.getMessage());
            }
        }
        return result > 0;
    }

    @Override
    public Photographer findByUserId(Long userId) {
        QueryWrapper<Photographer> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        Photographer photographer = photographerMapper.selectOne(wrapper);
        if (photographer != null) {
            // 动态计算完成订单数
            Integer completedCount = orderService.getCompletedOrderCount(photographer.getId());
            photographer.setOrderCount(completedCount != null ? completedCount : 0);
        }
        return photographer;
    }
    
    @Override
    public void updateOrderCount(Long photographerId) {
        Integer completedCount = orderService.getCompletedOrderCount(photographerId);
        Photographer photographer = photographerMapper.selectById(photographerId);
        if (photographer != null) {
            photographer.setOrderCount(completedCount != null ? completedCount : 0);
            photographerMapper.updateById(photographer);
        }
    }

    @Override
    public List<Photographer> getAllPhotographers() {
        return photographerMapper.selectList(null);
    }

    @Override
    public List<PhotographerNearbyVO> searchNearbyPhotographers(double latitude, double longitude, double radiusKm, int limit) {
        List<GeoDistanceResult> nearby = photographerGeoService.searchNearby(longitude, latitude, radiusKm, limit);
        if (nearby.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> ids = nearby.stream()
                .map(GeoDistanceResult::getPhotographerId)
                .collect(Collectors.toList());
        // 一次 IN 查询回表，避免 N+1
        Map<Long, Photographer> visibleById = photographerMapper.selectBatchIds(ids).stream()
                .filter(p -> p.getLocationVisible() == null || p.getLocationVisible() == 1)
                .collect(Collectors.toMap(Photographer::getId, p -> p, (a, b) -> a));

        List<PhotographerNearbyVO> result = new ArrayList<>();
        for (GeoDistanceResult g : nearby) { // 按 GEO 距离升序重排
            Photographer p = visibleById.get(g.getPhotographerId());
            if (p == null) {
                continue;
            }
            result.add(PhotographerNearbyVO.builder()
                    .id(p.getId())
                    .name(p.getName())
                    .phone(p.getPhone())
                    .avatar(p.getAvatar())
                    .working(p.getWorking())
                    .orderCount(p.getOrderCount())
                    .latitude(p.getLatitude())
                    .longitude(p.getLongitude())
                    .distanceKm(Math.round(g.getDistanceKm() * 100) / 100.0)
                    .build());
        }
        return result;
    }
}
