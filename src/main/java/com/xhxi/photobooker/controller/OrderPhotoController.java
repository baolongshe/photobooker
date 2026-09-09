package com.xhxi.photobooker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xhxi.photobooker.context.BaseContext;
import com.xhxi.photobooker.entity.Order;
import com.xhxi.photobooker.entity.OrderPhoto;
import com.xhxi.photobooker.entity.Portfolio;
import com.xhxi.photobooker.result.Result;
import com.xhxi.photobooker.service.OrderPhotoService;
import com.xhxi.photobooker.service.OrderService;
import com.xhxi.photobooker.service.PortfolioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/order-photo")
public class OrderPhotoController {

    @Autowired
    private OrderPhotoService orderPhotoService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private PortfolioService portfolioService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 批量保存照片
     */
    @PostMapping("/batch")
    public Result<Void> saveBatch(@RequestBody List<OrderPhoto> photos) {
        try {
            if (photos != null && !photos.isEmpty()) {
                for (OrderPhoto photo : photos) {
                    if (photo.getCreateTime() == null) {
                        photo.setCreateTime(LocalDateTime.now());
                    }
                    if (photo.getUpdateTime() == null) {
                        photo.setUpdateTime(LocalDateTime.now());
                    }
                    if (photo.getIsSelected() == null) {
                        photo.setIsSelected(0);
                    }
                    if (photo.getIsDelivered() == null) {
                        photo.setIsDelivered(0);
                    }
                    if (photo.getSortOrder() == null) {
                        photo.setSortOrder(0);
                    }
                }
                orderPhotoService.saveBatch(photos);
                
                // 同时保存到作品表
                savePhotosToPortfolio(photos);
            }
            return Result.success();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("批量保存照片失败：" + e.getMessage());
        }
    }

    /**
     * 将订单照片保存到作品集
     */
    private void savePhotosToPortfolio(List<OrderPhoto> photos) {
        if (photos == null || photos.isEmpty()) {
            return;
        }

        // 获取订单信息
        Long orderId = photos.get(0).getOrderId();
        Order order = orderService.selectByOrderId(orderId);
        
        if (order == null || order.getPhotographerId() == null) {
            return;
        }

        // 检查是否已经存在该订单的作品集（避免重复保存）
        // 这里简单处理：每次上传都创建新的作品集
        // 如果需要去重逻辑，可以在这里添加查询判断
        
        // 创建作品集
        Portfolio portfolio = new Portfolio();
        portfolio.setPhotographerId(order.getPhotographerId());
        portfolio.setTitle("订单 #" + orderId + " 照片集");
        portfolio.setDescription("订单交付的照片集合");
        portfolio.setCategory("订单交付");
        
        // 设置封面为第一张照片
        if (!photos.isEmpty() && photos.get(0).getPhotoUrl() != null) {
            portfolio.setCoverImage(photos.get(0).getPhotoUrl());
        }
        
        // 将所有照片URL收集为JSON数组
        List<String> photoUrls = new ArrayList<>();
        for (OrderPhoto photo : photos) {
            if (photo.getPhotoUrl() != null) {
                photoUrls.add(photo.getPhotoUrl());
            }
        }
        
        try {
            portfolio.setImageUrls(objectMapper.writeValueAsString(photoUrls));
        } catch (Exception e) {
            portfolio.setImageUrls("[]");
        }
        
        // 设置拍摄时间和地点（从订单获取）
        if (order.getShootingTime() != null) {
            portfolio.setShootingDate(java.sql.Timestamp.valueOf(order.getShootingTime()));
        }
        if (order.getShootingLocation() != null) {
            portfolio.setShootingLocation(order.getShootingLocation());
        }
        
        // 设置默认值
        portfolio.setStatus(1); // 已发布
        portfolio.setViewCount(0);
        portfolio.setLikeCount(0);
        portfolio.setIsFeatured(0); // 非精选
        portfolio.setSortWeight(0);
        
        // 保存到作品集
        portfolioService.savePortfolio(portfolio);
    }

    /**
     * 根据订单 ID 查询照片列表
     */
    @GetMapping("/order/{orderId}")
    public Result<List<OrderPhoto>> listByOrderId(@PathVariable Long orderId) {
        List<OrderPhoto> photos = orderPhotoService.listByOrderId(orderId);
        return Result.success(photos);
    }

    /**
     * 根据交付 ID 查询照片列表
     */
    @GetMapping("/delivery/{deliveryId}")
    public Result<List<OrderPhoto>> listByDeliveryId(@PathVariable Long deliveryId) {
        List<OrderPhoto> photos = orderPhotoService.listByDeliveryId(deliveryId);
        return Result.success(photos);
    }

    /**
     * 获取照片详情
     */
    @GetMapping("/{id}")
    public Result<OrderPhoto> getById(@PathVariable Long id) {
        OrderPhoto photo = orderPhotoService.getById(id);
        return Result.success(photo);
    }

    /**
     * 更新照片交付状态
     */
    @PutMapping("/{id}/delivery-status")
    public Result<Void> updateDeliveryStatus(
            @PathVariable Long id,
            @RequestParam Boolean isDelivered) {
        orderPhotoService.updateDeliveryStatus(id, isDelivered);
        return Result.success();
    }

    /**
     * 批量更新照片交付状态
     */
    @PutMapping("/batch-delivery/{deliveryId}")
    public Result<Void> updateBatchDeliveryStatus(
            @PathVariable Long deliveryId,
            @RequestParam Boolean isDelivered) {
        orderPhotoService.updateBatchDeliveryStatus(deliveryId, isDelivered);
        return Result.success();
    }

    /**
     * 获取照片统计信息
     */
    @GetMapping("/order/{orderId}/statistics")
    public Result<Map<String, Object>> getStatistics(@PathVariable Long orderId) {
        List<OrderPhoto> photos = orderPhotoService.listByOrderId(orderId);

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalCount", photos.size());
        statistics.put("deliveredCount", photos.stream()
                .filter(p -> p.getIsDelivered() == 1)
                .count());
        statistics.put("selectedCount", photos.stream()
                .filter(p -> p.getIsSelected() == 1)
                .count());

        return Result.success(statistics);
    }
}
