package com.xhxi.photobooker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xhxi.photobooker.context.BaseContext;
import com.xhxi.photobooker.entity.Order;
import com.xhxi.photobooker.entity.OrderDelivery;
import com.xhxi.photobooker.entity.OrderPhoto;
import com.xhxi.photobooker.entity.Portfolio;
import com.xhxi.photobooker.result.Result;
import com.xhxi.photobooker.service.OrderDeliveryService;
import com.xhxi.photobooker.service.OrderPhotoService;
import com.xhxi.photobooker.service.OrderService;
import com.xhxi.photobooker.service.PortfolioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/order-delivery")
public class OrderDeliveryController {

    @Autowired
    private OrderDeliveryService orderDeliveryService;

    @Autowired
    private PortfolioService portfolioService;
    @Autowired
    private OrderPhotoService orderPhotoService;
    @Autowired
    private OrderService orderService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 创建交付批次
     */
    @PostMapping("/batch")
    public Result<OrderDelivery> createDeliveryBatch(@RequestBody OrderDelivery delivery) {
        delivery.setPhotographerId(BaseContext.getCurrentId());
        OrderDelivery saved = orderDeliveryService.createDeliveryBatch(delivery);
        return Result.success(saved);
    }

    /**
     * 批量上传照片
     */
    @PostMapping("/photos/batch")
    public Result<Map<String, Object>> uploadPhotosBatch(
            @RequestParam("file") List<MultipartFile> files,
            @RequestParam("orderId") Long orderId,
            @RequestParam("deliveryId") Long deliveryId,
            @RequestParam(value = "tags", required = false) String tags) {

        try {
            List<OrderPhoto> savedPhotos = new ArrayList<>();
            String[] tagArray = tags != null ? tags.split(",") : new String[]{};

            // 获取当前批次已存在的照片数量
            List<OrderPhoto> existingPhotos = orderPhotoService.listByOrderId(orderId);
            int currentCount = existingPhotos.size();

            for (int i = 0; i < files.size(); i++) {
                MultipartFile file = files.get(i);
                if (file.isEmpty()) {
                    continue;
                }

                OrderPhoto photo = new OrderPhoto();
                photo.setOrderId(orderId);
                photo.setDeliveryId(deliveryId);
                photo.setPhotoNumber("P" + String.format("%04d", currentCount + i + 1));
                photo.setPhotoUrl(file.getOriginalFilename()); // 临时使用文件名，实际应由文件上传接口返回 URL
                photo.setFileSize(file.getSize());
                photo.setTags(tagArray);
                photo.setIsDelivered(0);
                photo.setIsSelected(0);
                photo.setSortOrder(currentCount + i);
                photo.setDescription("");

                savedPhotos.add(photo);
            }

            // 批量保存
            if (!savedPhotos.isEmpty()) {
                //保存到交付批次表中
                orderPhotoService.saveBatch(savedPhotos);
                
                // 保存到作品表中 - 需要将 OrderPhoto 转换为 Portfolio
                Order order = orderService.selectByOrderId(orderId);
                if (order != null && order.getPhotographerId() != null) {
                    List<Portfolio> portfolios = new ArrayList<>();
                    
                    // 为每张照片创建一个作品集条目（或者可以合并为一个作品集）
                    // 这里采用合并策略：将所有照片放入一个作品集中
                    Portfolio portfolio = new Portfolio();
                    portfolio.setPhotographerId(order.getPhotographerId());
                    portfolio.setTitle("订单 #" + orderId + " 交付作品");
                    portfolio.setDescription("订单交付的照片集");
                    portfolio.setCategory("订单交付");
                    
                    // 将标签数组转换为逗号分隔的字符串
                    if (tagArray.length > 0) {
                        portfolio.setTags(String.join(",", tagArray));
                    }
                    
                    // 设置封面为第一张照片
                    if (!savedPhotos.isEmpty()) {
                        portfolio.setCoverImage(savedPhotos.get(0).getPhotoUrl());
                    }
                    
                    // 将所有照片URL收集为JSON数组
                    List<String> photoUrls = new ArrayList<>();
                    for (OrderPhoto photo : savedPhotos) {
                        photoUrls.add(photo.getPhotoUrl());
                    }
                    portfolio.setImageUrls(objectMapper.writeValueAsString(photoUrls));
                    
                    // 设置拍摄时间和地点（从订单获取）
                    if (order.getShootingTime() != null) {
                        portfolio.setShootingDate(java.sql.Timestamp.valueOf(order.getShootingTime()));
                    }
                    if (order.getShootingLocation() != null) {
                        portfolio.setShootingLocation(order.getShootingLocation());
                    }
                    
                    portfolio.setStatus(1); // 已发布
                    portfolio.setViewCount(0);
                    portfolio.setLikeCount(0);
                    portfolio.setIsFeatured(0); // 非精选
                    portfolio.setSortWeight(0);
                    portfolio.setCreateTime(new Date());
                    portfolio.setUpdateTime(new Date());
                    
                    portfolios.add(portfolio);
                    
                    // 批量保存作品集 - 使用循环保存（PortfolioService 没有 saveBatch 方法）
                    for (Portfolio p : portfolios) {
                        portfolioService.savePortfolio(p);
                    }
                }
            }

            // 更新交付批次的照片数量
            OrderDelivery delivery = orderDeliveryService.getById(deliveryId);
            if (delivery != null) {
                delivery.setDeliveredCount(delivery.getDeliveredCount() + savedPhotos.size());
                delivery.setTotalCount(delivery.getTotalCount() + savedPhotos.size());
                if (delivery.getDeliveredCount() >= delivery.getTotalCount() && delivery.getTotalCount() > 0) {
                    delivery.setStatus("COMPLETED");
                } else if (delivery.getDeliveredCount() > 0) {
                    delivery.setStatus("PARTIAL");
                }
                delivery.setDeliveryTime(LocalDateTime.now());
                orderDeliveryService.updateById(delivery);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("count", savedPhotos.size());
            result.put("photos", savedPhotos);

            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("上传失败：" + e.getMessage());
        }
    }

    /**
     * 更新交付状态
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateDeliveryStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        OrderDelivery delivery = orderDeliveryService.getById(id);
        if (delivery != null) {
            delivery.setStatus(status);
            if ("COMPLETED".equals(status)) {
                delivery.setDeliveryTime(LocalDateTime.now());
            }
            orderDeliveryService.updateById(delivery);
        }
        return Result.success();
    }

    /**
     * 根据订单 ID 查询交付批次
     */
    @GetMapping("/order/{orderId}")
    public Result<List<OrderDelivery>> listByOrderId(@PathVariable Long orderId) {
        List<OrderDelivery> list = orderDeliveryService.listByOrderId(orderId);
        return Result.success(list);
    }

    /**
     * 获取交付批次详情
     */
    @GetMapping("/{id}")
    public Result<OrderDelivery> getById(@PathVariable Long id) {
        OrderDelivery delivery = orderDeliveryService.getById(id);
        return Result.success(delivery);
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
}
