package com.xhxi.photobooker.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xhxi.photobooker.entity.OrderDelivery;
import com.xhxi.photobooker.entity.OrderPhoto;
import com.xhxi.photobooker.mapper.OrderDeliveryMapper;
import com.xhxi.photobooker.mapper.OrderPhotoMapper;
import com.xhxi.photobooker.service.OrderDeliveryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderDeliveryServiceImpl extends ServiceImpl<OrderDeliveryMapper, OrderDelivery>
        implements OrderDeliveryService {

    @Autowired
    private OrderPhotoMapper orderPhotoMapper;

    @Override
    public OrderDelivery createDeliveryBatch(OrderDelivery delivery) {
        delivery.setCreateTime(LocalDateTime.now());

        // 生成交付批次号
        String batchNumber = "D" + System.currentTimeMillis();
        delivery.setDeliveryBatch(batchNumber);

        if (delivery.getTotalCount() == null) {
            delivery.setTotalCount(0);
        }
        if (delivery.getDeliveredCount() == null) {
            delivery.setDeliveredCount(0);
        }
        if (delivery.getStatus() == null) {
            delivery.setStatus("PENDING");
        }

        save(delivery);
        return delivery;
    }

    @Override
    public List<OrderDelivery> listByOrderId(Long orderId) {
        LambdaQueryWrapper<OrderDelivery> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderDelivery::getOrderId, orderId)
                .orderByDesc(OrderDelivery::getCreateTime);
        List<OrderDelivery> deliveries = list(wrapper);
        
        // 为每个交付批次动态计算已交付数量
        for (OrderDelivery delivery : deliveries) {
            LambdaQueryWrapper<OrderPhoto> photoWrapper = new LambdaQueryWrapper<>();
            photoWrapper.eq(OrderPhoto::getDeliveryId, delivery.getId())
                       .eq(OrderPhoto::getIsDelivered, 1);
            long deliveredCount = orderPhotoMapper.selectCount(photoWrapper);
            delivery.setDeliveredCount((int)deliveredCount);
        }
        
        return deliveries;
    }
}
