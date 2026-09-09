 package com.xhxi.photobooker.service;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xhxi.photobooker.entity.Order;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public interface OrderService {
    /*用户查询订单*/
    List<Order> listUserOrders(Long userId);

    /*用户分页查询订单*/
    com.xhxi.photobooker.result.PageResult<Order> listUserOrdersWithPage(Long userId,
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Order> page,
        String status);

    /*创建订单*/
    Order createOrder(Order order);

    /*根据orderId查询订单*/
    Order selectByOrderId(Long orderId);
    /*完成订单*/
    boolean finishOrder(Long orderId);
    
    /**
     * 摄影师确认订单，开始拍摄
     */
    boolean startShooting(Long orderId);
    
    /**
     * 摄影师完成拍摄，等待用户确认
     */
    boolean pendingConfirm(Long orderId);
    
    /**
     * 用户确认完成订单
     */
    boolean confirmComplete(Long orderId);
    /*
    * 摄影师拒绝订单
    * */
    boolean rejectOrder(Long orderId,Long photographerId,String reason);
    /**
     * 自动完成超时订单（2 天后）
     */
    void autoCompleteExpiredOrders();
    
    /*取消订单*/
    Order cancelOrder(Long id, Long userId);

    /*摄影师查询订单*/
    List<Order> listOrdersByPhotographerId(Long photographerId);
    
    /*获取摄影师完成的订单数*/
    Integer getCompletedOrderCount(Long photographerId);
    
    /*获取所有订单*/
    List<Order> getAllOrders();

    /*获取近期订单*/
    List<Order> getRecentOrders(int limit);

    /*取消过期订单*/
    void cancelExpireOrder(Long orderId);

    /**
     * 处理退款后的订单状态更新
     * @param orderId 订单ID
     * @return 是否成功
     */
    boolean handleRefundOrder(Long orderId);

    /**
     * 完成退款，更新订单状态为已退款
     * @param orderId 订单ID
     * @return 是否成功
     */
    boolean completeRefundOrder(Long orderId);

}
