package com.xhxi.photobooker.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xhxi.photobooker.entity.OrderDelivery;
import java.util.List;

public interface OrderDeliveryService extends IService<OrderDelivery> {

    /**
     * 创建交付批次
     */
    OrderDelivery createDeliveryBatch(OrderDelivery delivery);

    /**
     * 根据订单 ID 查询交付批次
     */
    List<OrderDelivery> listByOrderId(Long orderId);
}
