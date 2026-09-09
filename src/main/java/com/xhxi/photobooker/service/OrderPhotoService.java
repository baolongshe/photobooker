package com.xhxi.photobooker.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xhxi.photobooker.entity.OrderPhoto;
import java.util.List;

public interface OrderPhotoService extends IService<OrderPhoto> {

    /**
     * 根据订单 ID 查询照片列表
     */
    List<OrderPhoto> listByOrderId(Long orderId);

    /**
     * 根据交付 ID 查询照片列表
     */
    List<OrderPhoto> listByDeliveryId(Long deliveryId);

    /**
     * 批量保存照片
     */
    boolean saveBatch(List<OrderPhoto> photos);

    /**
     * 批量更新照片交付状态
     */
    void updateBatchDeliveryStatus(Long deliveryId, Boolean isDelivered);

    /**
     * 更新照片交付状态
     */
    boolean updateDeliveryStatus(Long id, Boolean isDelivered);
}
