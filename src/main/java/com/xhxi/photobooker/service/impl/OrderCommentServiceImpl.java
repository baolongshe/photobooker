package com.xhxi.photobooker.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xhxi.photobooker.entity.Order;
import com.xhxi.photobooker.entity.OrderComment;
import com.xhxi.photobooker.enums.OrderStatus;
import java.time.LocalDateTime;
import com.xhxi.photobooker.mapper.OrderCommentMapper;
import com.xhxi.photobooker.service.OrderCommentService;
import com.xhxi.photobooker.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class OrderCommentServiceImpl extends ServiceImpl<OrderCommentMapper, OrderComment> implements OrderCommentService {

    @Autowired
    private OrderService orderService;

    @Override
    public OrderComment createComment(OrderComment comment) {
        if (comment.getRating() == null || comment.getRating() < 1 || comment.getRating() > 5) {
            throw new RuntimeException("评分必须在1-5之间");
        }

        Order order = orderService.selectByOrderId(comment.getOrderId());
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!comment.getUserId().equals(order.getUserId())) {
            throw new RuntimeException("无权评价该订单");
        }
        if (!OrderStatus.COMPLETED.equals(order.getStatus())) {
            throw new RuntimeException("只能评价已完成的订单");
        }

        comment.setStatus(0);
        comment.setCreateTime(LocalDateTime.now());
        save(comment);
        return comment;
    }

    @Override
    public OrderComment getByOrderId(Long orderId) {
        LambdaQueryWrapper<OrderComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderComment::getOrderId, orderId)
                .eq(OrderComment::getStatus, 0);
        return getOne(wrapper);
    }

    @Override
    public List<OrderComment> listByUserId(Long userId) {
        LambdaQueryWrapper<OrderComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderComment::getUserId, userId)
                .eq(OrderComment::getStatus, 0)
                .orderByDesc(OrderComment::getCreateTime);
        return list(wrapper);
    }

    @Override
    public List<OrderComment> listByPhotographerId(Long photographerId) {
        LambdaQueryWrapper<OrderComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderComment::getPhotographerId, photographerId)
                .eq(OrderComment::getStatus, 0)
                .orderByDesc(OrderComment::getCreateTime);
        return list(wrapper);
    }

    @Override
    public Double getAverageRating(Long photographerId) {
        List<OrderComment> comments = listByPhotographerId(photographerId);
        if (comments.isEmpty()) {
            return 0.0;
        }
        double total = comments.stream()
                .mapToDouble(OrderComment::getRating)
                .sum();
        return total / comments.size();
    }
}
