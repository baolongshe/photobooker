package com.xhxi.photobooker.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xhxi.photobooker.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {
    // 根据用户ID查询订单列表
    List<Order> selectByUserId(@Param("user_id") Long userId);

    // 根据摄影师ID查询订单列表
    List<Order> selectOrderByPhotographerId(@Param("photographer_id") Long photographerId);
    
    // 统计摄影师完成的订单数
    Integer countCompletedOrdersByPhotographerId(@Param("photographer_id") Long photographerId);
}