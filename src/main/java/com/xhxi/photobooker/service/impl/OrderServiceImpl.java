package com.xhxi.photobooker.service.impl;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.xhxi.photobooker.entity.*;
import com.xhxi.photobooker.enums.OrderStatus;
import com.xhxi.photobooker.mapper.OrderMapper;
import com.xhxi.photobooker.mapper.OrderPhotoMapper;
import com.xhxi.photobooker.mapper.PhotographerMapper;
import com.xhxi.photobooker.mapper.PhotographerPackageMapper;
import com.xhxi.photobooker.service.AfterSalesServiceService;
import com.xhxi.photobooker.service.MessageService;
import com.xhxi.photobooker.service.OrderService;
import com.xhxi.photobooker.service.PhotographerService;
import com.xhxi.photobooker.service.UserService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class OrderServiceImpl implements OrderService {
    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);

    private static final String ORDER_TIMEOUT_ZSET_KEY = "order:timeout:pending";
    private static final long ORDER_TIMEOUT_MS = 15 * 60 * 1000; // 15分钟

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private PhotographerMapper photographerMapper;

    @Autowired
    private OrderPhotoMapper orderPhotoMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private UserService userService;

    @Autowired
    private AlipayClient alipayClient;

    @Autowired
    private MessageService messageService;

    @Autowired
    @Lazy
    private AfterSalesServiceService afterSalesServiceService;

    /*用户查询所有订单*/

    public List<Order> listUserOrders(Long userId) {
        List<Order> orders = orderMapper.selectByUserId(userId);

        // 为所有订单添加用户头像和照片信息
        for (Order order : orders) {
            // 1. 获取用户头像
            User user = userService.findUserById(order.getUserId());
            if (user != null && user.getAvatar() != null) {
                order.setUserAvatar(user.getAvatar());
                logger.debug("订单 {} 的用户头像：{}", order.getId(), user.getAvatar());
            } else {
                // 设置默认头像
                order.setUserAvatar("https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png");
            }

            // 2. 为已完成的订单添加交付的照片
            if (OrderStatus.COMPLETED.equals(order.getStatus())) {
                List<OrderPhoto> photos = orderPhotoMapper.selectList(
                    new QueryWrapper<OrderPhoto>()
                        .eq("order_id", order.getId())
                        .eq("is_delivered", 1)
                        .orderByAsc("sort_order")
                );

                if (!photos.isEmpty()) {
                    List<String> photoUrls = photos.stream()
                        .map(OrderPhoto::getPhotoUrl)
                        .collect(java.util.stream.Collectors.toList());
                    order.setDeliveredPhotos(photoUrls);
                }
            }
        }

        return orders;
    }

    @Override
    public com.xhxi.photobooker.result.PageResult<Order> listUserOrdersWithPage(Long userId,
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<Order> page,
            String status) {
        // 创建查询条件
        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);

        //zzx新增：状态筛选
        if (status != null && !status.trim().isEmpty()) {
            try {
                OrderStatus orderStatus = OrderStatus.valueOf(status);
                queryWrapper.eq("status", orderStatus);
            } catch (IllegalArgumentException e) {
                // 状态码无效，忽略筛选条件
                logger.warn("无效的订单状态: {}", status);
            }
        }

        queryWrapper.orderByDesc("create_time");  // 按创建时间倒序

        // 执行分页查询
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Order> result =
            orderMapper.selectPage(page, queryWrapper);

        // 获取分页数据
        List<Order> orders = result.getRecords();
        long total = result.getTotal();

        // 为订单添加用户头像和照片信息（与 listUserOrders 相同逻辑）
        for (Order order : orders) {
            // 1. 获取用户头像
            User user = userService.findUserById(order.getUserId());
            if (user != null && user.getAvatar() != null) {
                order.setUserAvatar(user.getAvatar());
            } else {
                order.setUserAvatar("https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png");
            }

            // 2. 为已完成的订单添加交付的照片
            if (OrderStatus.COMPLETED.equals(order.getStatus())) {
                List<OrderPhoto> photos = orderPhotoMapper.selectList(
                    new QueryWrapper<OrderPhoto>()
                        .eq("order_id", order.getId())
                        .eq("is_delivered", 1)
                        .orderByAsc("sort_order")
                );

                if (!photos.isEmpty()) {
                    List<String> photoUrls = photos.stream()
                        .map(OrderPhoto::getPhotoUrl)
                        .collect(java.util.stream.Collectors.toList());
                    order.setDeliveredPhotos(photoUrls);
                }
            }
        }

        return com.xhxi.photobooker.result.PageResult.success(total, orders);
    }




    @Transactional
    public Order createOrder(Order order) {
        order.setCreateTime(LocalDateTime.now());
        order.setStatus(OrderStatus.PENDING);

        // 检查摄影师档期
        boolean isAvailable = checkPhotographerAvailability(
                order.getPhotographerId(),
                order.getShootingTime()
        );

        if (!isAvailable) {
            throw new RuntimeException(" 摄影师该时段不可用");
        }

        orderMapper.insert(order);

        // 注册订单超时到 Redis ZSet（统一在 Service 层处理，Controller 和 AI Agent 创建都生效）
        redisTemplate.opsForZSet().add(
            ORDER_TIMEOUT_ZSET_KEY,
            order.getId().toString(),
            System.currentTimeMillis() + ORDER_TIMEOUT_MS
        );

        return order;
    }

    @Override
    public Order selectByOrderId(Long   orderId) {
        return orderMapper.selectById(orderId);
    }


    @Override
    @Transactional
    public boolean finishOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        order.setStatus(OrderStatus.CONFIRMED);
        orderMapper.updateById(order);

        // 更新摄影师的完成订单数
        updatePhotographerOrderCount(order.getPhotographerId());

        // 支付成功，从超时ZSET中移除
        redisTemplate.opsForZSet().remove(ORDER_TIMEOUT_ZSET_KEY, orderId.toString());

        return true;
    }

    @Override
    @Transactional
    public boolean startShooting(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !OrderStatus.CONFIRMED.equals(order.getStatus())) {
            throw new RuntimeException("订单状态不可开始拍摄");
        }
        order.setStatus(OrderStatus.IN_PROGRESS);
        orderMapper.updateById(order);
        return true;
    }

    @Override
    @Transactional
    public boolean pendingConfirm(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !OrderStatus.IN_PROGRESS.equals(order.getStatus())) {
            throw new RuntimeException("订单状态不可提交确认");
        }
        order.setStatus(OrderStatus.PENDING_CONFIRM);
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        return true;
    }

    @Override
    @Transactional
    public boolean confirmComplete(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !OrderStatus.PENDING_CONFIRM.equals(order.getStatus())) {
            throw new RuntimeException("订单状态不可确认");
        }
        order.setStatus(OrderStatus.COMPLETED);
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);

        // 更新摄影师的完成订单数
        updatePhotographerOrderCount(order.getPhotographerId());

        return true;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public boolean rejectOrder(Long orderId, Long photographerId, String reason) {
        Order order = orderMapper.selectById(orderId);
        if(order == null || !photographerId.equals(order.getPhotographerId())){
            throw new RuntimeException("订单不存在或无权限操作");
        }
        // 只有已确认的订单可以拒绝
        if (!OrderStatus.CONFIRMED.equals(order.getStatus())) {
            throw new RuntimeException("该订单状态不可拒绝");
        }
        order.setStatus(OrderStatus.REJECTED);
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);

        // TODO: 发送拒绝通知
        sendRejectNotification(order, reason);
        // TODO: 自动触发退款流程
        triggerRefund(order);

        return true;
    }

    /**
     * 发送订单拒绝通知给用户
     */
    private void sendRejectNotification(Order order, String reason) {
        try {
            com.xhxi.photobooker.entity.Message entityMsg = new com.xhxi.photobooker.entity.Message();
            entityMsg.setSenderId(0L); // 0 表示系统
            entityMsg.setSenderRole("system");
            entityMsg.setReceiverId(order.getUserId());
            entityMsg.setReceiverRole("user");
            entityMsg.setOrderId(order.getId());
            entityMsg.setContent("摄影师已拒绝您的订单。原因：" + (reason != null ? reason : "未填写"));
            entityMsg.setType("system");
            entityMsg.setCreateTime(new java.util.Date());
            entityMsg.setIsDelivered(0);
            messageService.saveMessage(entityMsg);

            logger.info("订单拒绝通知已保存到数据库，订单ID: {}", order.getId());
        } catch (Exception e) {
            // 通知失败不影响订单拒绝流程
            logger.error("发送拒绝通知失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 自动触发退款流程
     */
    private void triggerRefund(Order order) {
        try {
            java.util.List<com.xhxi.photobooker.entity.AfterSalesService> services =
                    afterSalesServiceService.listByOrderId(order.getId());

            com.xhxi.photobooker.entity.AfterSalesService refundService = null;

            // 查找已存在的退款申请
            for (com.xhxi.photobooker.entity.AfterSalesService service : services) {
                if ("REFUND".equalsIgnoreCase(service.getServiceType())) {
                    refundService = service;
                    break;
                }
            }

            // 如果没有退款记录，自动创建一条
            if (refundService == null) {
                refundService = new com.xhxi.photobooker.entity.AfterSalesService();
                refundService.setOrderId(order.getId());
                refundService.setUserId(order.getUserId());
                refundService.setPhotographerId(order.getPhotographerId());
                refundService.setServiceType("REFUND");
                refundService.setStatus("PROCESSING");
                refundService.setRefundAmount(order.getTotalPrice());
                refundService.setRefundReason("摄影师拒绝拍摄，自动退款");
                refundService.setHandlerResponse("摄影师拒绝订单，系统自动触发退款");
                refundService.setCreateTime(LocalDateTime.now());
                afterSalesServiceService.save(refundService);
            } else {
                // 更新已有记录为处理中
                refundService.setStatus("PROCESSING");
                refundService.setRefundAmount(order.getTotalPrice());
                afterSalesServiceService.updateById(refundService);
            }

            // 调用支付宝退款接口
            AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
            String bizContent = "{" +
                    "\"out_trade_no\":\"" + order.getId() + "\"," +
                    "\"refund_amount\":\"" + order.getTotalPrice() + "\"," +
                    "\"refund_reason\":\"摄影师拒绝拍摄\"" +
                    "}";
            request.setBizContent(bizContent);

            AlipayTradeRefundResponse alipayResponse = alipayClient.execute(request);

            if (!alipayResponse.isSuccess()) {
                throw new RuntimeException("支付宝退款失败：" + alipayResponse.getMsg() + " - " + alipayResponse.getSubMsg());
            }

            // 退款成功，更新售后记录
            refundService.setStatus("COMPLETED");
            refundService.setHandleTime(LocalDateTime.now());
            refundService.setHandlerResponse("支付宝退款成功，交易号：" + alipayResponse.getTradeNo());
            afterSalesServiceService.updateById(refundService);

            // 更新订单状态为已退款
            order.setStatus(OrderStatus.REFUNDED);
            order.setUpdateTime(LocalDateTime.now());
            orderMapper.updateById(order);

            logger.info("订单 #{} 自动退款成功，金额：{}", order.getId(), order.getTotalPrice());
        } catch (AlipayApiException e) {
            logger.error("支付宝退款异常: {}", e.getMessage(), e);
        } catch (Exception e) {
            logger.error("触发退款流程失败: {}", e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void autoCompleteExpiredOrders() {
        // 查询所有待确认完成且超过 2 天的订单
        QueryWrapper<Order> wrapper = new QueryWrapper<>();
        wrapper.eq("status", OrderStatus.PENDING_CONFIRM.getCode())
               .lt("update_time", LocalDateTime.now().minus(2, ChronoUnit.DAYS));

        List<Order> expiredOrders = orderMapper.selectList(wrapper);
        for (Order order : expiredOrders) {
            order.setStatus(OrderStatus.COMPLETED);
            orderMapper.updateById(order);
            updatePhotographerOrderCount(order.getPhotographerId());
        }
    }

    public Order cancelOrder(Long orderId, Long userId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在或无权限操作");
        }

        if (!userId.equals(order.getUserId())) {
            throw new RuntimeException("订单不存在或无权限操作");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("只有待支付的订单可以取消");
        }

        order.setStatus(OrderStatus.CANCELED);
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);

        // 【新增】释放摄影师档期
        releasePhotographerSchedule(order.getPhotographerId(), order.getShootingTime());

        // 从超时ZSET中移除
        redisTemplate.opsForZSet().remove(ORDER_TIMEOUT_ZSET_KEY, orderId.toString());

        return order;
    }


    /*摄影师查询所有订单*/
    @Override
    public List<Order> listOrdersByPhotographerId(Long photographerId) {
        return orderMapper.selectOrderByPhotographerId(photographerId);
    }

    /*获取摄影师完成的订单数*/
    @Override
    public Integer getCompletedOrderCount(Long photographerId) {
        return orderMapper.countCompletedOrdersByPhotographerId(photographerId);
    }

    /*获取所有订单*/
    @Override
    public List<Order> getAllOrders() {
        return orderMapper.selectList(null);
    }

    @Override
    public List<Order> getRecentOrders(int limit) {
        limit = Math.max(1, Math.min(limit, 100));
        QueryWrapper<Order> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("create_time").last("LIMIT " + limit);
        return orderMapper.selectList(wrapper);
    }

    /*取消过期订单（幂等：仅将 PENDING 状态的订单取消）*/
    @Override
    @Transactional
    public void cancelExpireOrder(Long orderId) {
        // 原子性更新：只有状态为 PENDING 时才更新为 CANCELED
        // 避免 Redis 过期通知和 ZSet 定时扫描同时触发导致的竞态
        Order updateEntity = new Order();
        updateEntity.setStatus(OrderStatus.CANCELED);
        updateEntity.setUpdateTime(LocalDateTime.now());

        int affected = orderMapper.update(updateEntity,
            new UpdateWrapper<Order>()
                .eq("id", orderId)
                .eq("status", OrderStatus.PENDING)
        );

        if (affected == 0) {
            logger.info("订单 {} 已被处理，跳过重复取消", orderId);
            return;
        }

        // 从 ZSet 移除
        redisTemplate.opsForZSet().remove(ORDER_TIMEOUT_ZSET_KEY, orderId.toString());

        // 释放摄影师档期
        Order order = orderMapper.selectById(orderId);
        if (order != null) {
            releasePhotographerSchedule(order.getPhotographerId(), order.getShootingTime());
            logger.info("订单 {} 超时取消，摄影师 {} 档期已释放", orderId, order.getPhotographerId());
        }
    }

    /**
     * 释放摄影师档期（使用Redis分布式锁防止并发）
     */
    private void releasePhotographerSchedule(Long photographerId, LocalDateTime shootingTime) {
        String lockKey = "lock:photographer:schedule:" + photographerId;
        String lockValue = String.valueOf(System.currentTimeMillis());

        try {
            Boolean locked = redisTemplate.opsForValue().setIfAbsent(
                lockKey, lockValue, 5, java.util.concurrent.TimeUnit.SECONDS
            );

            if (Boolean.TRUE.equals(locked)) {
                logger.info("摄影师 {} 档期锁获取成功，开始释放档期", photographerId);

                String scheduleCacheKey = "photographer:schedule:" + photographerId;
                redisTemplate.delete(scheduleCacheKey);

            } else {
                logger.warn("摄影师 {} 档期锁获取失败，可能有其他线程正在处理", photographerId);
            }
        } catch (Exception e) {
            logger.error("释放摄影师 {} 档期失败", photographerId, e);
        } finally {
            String currentLockValue = (String) redisTemplate.opsForValue().get(lockKey);
            if (lockValue.equals(currentLockValue)) {
                redisTemplate.delete(lockKey);
            }
        }
    }

    /**
     * 更新摄影师的完成订单数（同时更新Redis排行榜ZSET）
     */
    private void updatePhotographerOrderCount(Long photographerId) {
        Integer completedCount = getCompletedOrderCount(photographerId);
        Photographer photographer = photographerMapper.selectById(photographerId);
        if (photographer != null) {
            photographer.setOrderCount(completedCount);
            photographerMapper.updateById(photographer);
            
            // 同步更新Redis ZSET排行榜
            String rankingKey = "ranking:hot:photographer";
            redisTemplate.opsForZSet().add(rankingKey, photographerId.toString(), completedCount);
            redisTemplate.expire(rankingKey, 7, java.util.concurrent.TimeUnit.DAYS);
            
            logger.info("摄影师 {} 订单数更新为 {}, 排行榜已同步", photographerId, completedCount);
        }
    }

    @Override
    @Transactional
    public boolean handleRefundOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        
        // 只有已支付或已确认的订单可以退款
        if (!OrderStatus.CONFIRMED.equals(order.getStatus()) && 
            !OrderStatus.IN_PROGRESS.equals(order.getStatus()) &&
            !OrderStatus.PENDING_UPLOAD.equals(order.getStatus()) &&
            !OrderStatus.PENDING_CONFIRM.equals(order.getStatus()) &&
            !OrderStatus.COMPLETED.equals(order.getStatus())) {
            throw new RuntimeException("该订单状态不支持退款");
        }
        
        // 将订单状态设置为退款中
        order.setStatus(OrderStatus.REFUNDING);
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        
        return true;
    }

    /**
     * 完成退款，更新订单状态为已退款
     */
    @Override
    @Transactional
    public boolean completeRefundOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        
        if (!OrderStatus.REFUNDING.equals(order.getStatus())) {
            throw new RuntimeException("订单不在退款中状态");
        }
        
        // 将订单状态设置为已退款
        order.setStatus(OrderStatus.REFUNDED);
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        
        return true;
    }

    private boolean checkPhotographerAvailability(Long photographerId, LocalDateTime shootingTime) {
        //检查摄影师是否在工作时间
        Photographer photographer = photographerMapper.selectById(photographerId);
        if(photographer ==null || 1!=photographer.getWorking()){
            return false;
        }
        LocalDateTime startTime = shootingTime.minusHours(2); // 向前检查2小时
        LocalDateTime endTime = shootingTime.plusHours(2); // 向后检查时长+2小时

        // 检查摄影师在指定时间是否已有订单
        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("photographer_id", photographerId)
                .between("shooting_time", startTime,endTime)
                .notIn("status", "CANCELED", "FAILED");
        logger.debug("已存在订单：{}", orderMapper.selectList(queryWrapper));
        return !orderMapper.exists(queryWrapper);
    }
}