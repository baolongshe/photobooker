package com.xhxi.photobooker.controller;

import com.xhxi.photobooker.context.BaseContext;
import com.xhxi.photobooker.entity.Order;
import com.xhxi.photobooker.entity.Photographer;
import com.xhxi.photobooker.entity.User;
import com.xhxi.photobooker.result.PageResult;
import com.xhxi.photobooker.result.Result;
import com.xhxi.photobooker.service.OrderService;
import com.xhxi.photobooker.service.PhotographerService;
import com.xhxi.photobooker.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private PhotographerService photographerService;

    @Autowired
    private UserService userService;

    @PostMapping("/user/create")
    public Result<Order> createOrder(@RequestBody Order order) {
        Long currentUserId = BaseContext.getCurrentId();
        if (currentUserId == null) {
            return Result.error("请先登录");
        }
        order.setUserId(currentUserId);
        Order created = orderService.createOrder(order);
        return Result.success(created);
    }

    @GetMapping("/user")
    public Result<PageResult<Order>> listUserOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status
    ) {
        Long currentUserId = BaseContext.getCurrentId();
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Order> orderPage =
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, pageSize);

        PageResult<Order> result = orderService.listUserOrdersWithPage(currentUserId, orderPage, status);
        return Result.success(result);
    }

    @PutMapping("/{id}/cancel")
    public Result<Order> cancelOrder(@PathVariable Long id) {
        Order order = orderService.cancelOrder(id, BaseContext.getCurrentId());
        return Result.success(order);
    }

    @PostMapping("/{orderId}/chat")
    public Result<String> startChat(
            @PathVariable Long orderId,
            HttpServletRequest request
    ) {
        Long currentUserId = BaseContext.getCurrentId();
        Order order = orderService.selectByOrderId(orderId);
        if (order == null) {
            return Result.error("订单不存在");
        }

        if (!currentUserId.equals(order.getUserId()) && !currentUserId.equals(order.getPhotographerId())) {
            return Result.error("无权限");
        }

        String wsUrl = "ws://" + request.getServerName() + ":" + request.getServerPort()
                + "/ws/chat/user/" + currentUserId + "?orderId=" + orderId;

        return Result.success(wsUrl);
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> getOrderDetail(@PathVariable Long id) {
        Order order = orderService.selectByOrderId(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        Photographer photographer = photographerService.findPhotographerById(order.getPhotographerId());
        User user = userService.findUserById(order.getUserId());
        Map<String, Object> result = new HashMap<>();
        result.put("order", order);
        result.put("photographer", photographer);
        Map<String, Object> safeUser = new HashMap<>();
        safeUser.put("id", user.getId());
        safeUser.put("username", user.getUsername());
        safeUser.put("avatar", user.getAvatar());
        safeUser.put("realName", user.getRealName());
        safeUser.put("phone", user.getPhone());
        result.put("user", safeUser);
        return Result.success(result);
    }

    @PutMapping("/{id}/complete")
    public Result<Boolean> completeOrder(@PathVariable Long id) {
        try {
            boolean result = orderService.finishOrder(id);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("完成订单失败：" + e.getMessage());
        }
    }

    @PutMapping("/{id}/start-shooting")
    public Result<Boolean> startShooting(@PathVariable Long id) {
        try {
            boolean result = orderService.startShooting(id);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("开始拍摄失败：" + e.getMessage());
        }
    }

    @PutMapping("/{id}/pending-confirm")
    public Result<Boolean> pendingConfirm(@PathVariable Long id) {
        try {
            boolean result = orderService.pendingConfirm(id);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("提交确认失败：" + e.getMessage());
        }
    }

    @PutMapping("/{id}/confirm-complete")
    public Result<Boolean> confirmComplete(@PathVariable Long id) {
        try {
            boolean result = orderService.confirmComplete(id);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("确认完成失败：" + e.getMessage());
        }
    }

    @GetMapping("/list")
    public Result<List<Order>> getAllOrders() {
        try {
            List<Order> orders = orderService.getAllOrders();
            return getListResult(orders);
        } catch (Exception e) {
            return Result.error("获取订单列表失败: " + e.getMessage());
        }
    }

    // 仪表盘订单数据（近5条订单数据）
    @GetMapping("/recent")
    public Result<List<Order>> getRecentOrders(@RequestParam(defaultValue = "5") int limit){
        List<Order> orders = orderService.getRecentOrders(limit);
        return getListResult(orders);
    }

    private Result<List<Order>> getListResult(List<Order> orders) {
        for (Order order : orders) {
            // 查用户
            User user = userService.findUserById(order.getUserId());
            order.setUserName(user != null ? user.getUsername() : "-");
            // 查摄影师
            Photographer photographer = photographerService.findPhotographerById(order.getPhotographerId());
            order.setPhotographerName(photographer != null ? photographer.getName() : "-");
        }

        return Result.success(orders);
    }

    /*拒绝订单*/
    @PutMapping("/{id}/reject")
    public Result<Boolean> rejectOrder(@PathVariable Long id,
                                       @RequestParam String reason) {
        try{
            boolean result = orderService.rejectOrder(id, BaseContext.getCurrentId(), reason);
            return Result.success(result);
        }catch (Exception e){
            return Result.error("拒绝订单失败：" + e.getMessage());
        }
    }
}