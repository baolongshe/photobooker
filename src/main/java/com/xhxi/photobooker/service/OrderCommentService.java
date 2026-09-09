package com.xhxi.photobooker.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xhxi.photobooker.entity.OrderComment;


import java.util.List;

public interface OrderCommentService extends IService<OrderComment> {
    /**
     * 创建评价
     */
    OrderComment createComment(OrderComment comment);

    /**
     * 根据订单 ID 查询评价
     */
    OrderComment getByOrderId(Long orderId);

    /**
     * 根据用户 ID 查询评价列表
     */
    List<OrderComment> listByUserId(Long userId);

    /**
     * 根据摄影师 ID 查询评价列表
     */
    List<OrderComment> listByPhotographerId(Long photographerId);

    /**
     * 计算摄影师平均评分
     */
    Double getAverageRating(Long photographerId);
}

