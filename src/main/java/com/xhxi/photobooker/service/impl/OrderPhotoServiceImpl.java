package com.xhxi.photobooker.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xhxi.photobooker.entity.OrderDelivery;
import com.xhxi.photobooker.entity.OrderPhoto;
import com.xhxi.photobooker.mapper.OrderPhotoMapper;
import com.xhxi.photobooker.service.OrderDeliveryService;
import com.xhxi.photobooker.service.OrderPhotoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderPhotoServiceImpl extends ServiceImpl<OrderPhotoMapper, OrderPhoto>
        implements OrderPhotoService {

    @Autowired
    private OrderDeliveryService orderDeliveryService;

    @Override
    public List<OrderPhoto> listByOrderId(Long orderId) {
        LambdaQueryWrapper<OrderPhoto> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderPhoto::getOrderId, orderId)
                .eq(OrderPhoto::getIsDelivered, 1)  // 只查询已交付的照片
                .orderByAsc(OrderPhoto::getSortOrder)
                .orderByAsc(OrderPhoto::getPhotoNumber);
        return list(wrapper);
    }

    @Override
    public List<OrderPhoto> listByDeliveryId(Long deliveryId) {
        LambdaQueryWrapper<OrderPhoto> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderPhoto::getDeliveryId, deliveryId)
                .orderByAsc(OrderPhoto::getSortOrder)
                .orderByAsc(OrderPhoto::getPhotoNumber);
        return list(wrapper);
    }

    @Override
    public boolean saveBatch(List<OrderPhoto> photos) {
        return super.saveBatch(photos);
    }

    @Override
    public boolean updateDeliveryStatus(Long id, Boolean isDelivered) {
        OrderPhoto photo = getById(id);
        if (photo != null) {
            photo.setIsDelivered(isDelivered ? 1 : 0);
            photo.setUpdateTime(LocalDateTime.now());
            return updateById(photo);
        }
        return false;
    }

    @Override
    public void updateBatchDeliveryStatus(Long deliveryId, Boolean isDelivered) {
        // 1. 更新照片的交付状态
        LambdaUpdateWrapper<OrderPhoto> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(OrderPhoto::getDeliveryId, deliveryId)
               .set(OrderPhoto::getIsDelivered, isDelivered ? 1 : 0)
               .set(OrderPhoto::getUpdateTime, LocalDateTime.now());
        update(wrapper);
        
        // 2. 计算已交付数量
        LambdaQueryWrapper<OrderPhoto> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.eq(OrderPhoto::getDeliveryId, deliveryId)
                   .eq(OrderPhoto::getIsDelivered, isDelivered ? 1 : 0);
        long deliveredCount = baseMapper.selectCount(countWrapper);
        
        // 3. 更新交付批次表
        OrderDelivery delivery = orderDeliveryService.getById(deliveryId);
        if (delivery != null) {
            delivery.setDeliveredCount((int)deliveredCount);
            // 如果所有照片都已交付，自动更新状态为 COMPLETED
            if (deliveredCount > 0 && deliveredCount >= delivery.getTotalCount()) {
                delivery.setStatus("COMPLETED");
                delivery.setDeliveryTime(LocalDateTime.now());
            } else if (deliveredCount > 0) {
                delivery.setStatus("PARTIAL");
            }
            orderDeliveryService.updateById(delivery);
        }
    }
}
