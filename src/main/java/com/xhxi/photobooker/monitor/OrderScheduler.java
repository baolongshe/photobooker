package com.xhxi.photobooker.monitor;

import com.xhxi.photobooker.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class OrderScheduler {

    @Autowired
    private OrderService orderService;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final String ORDER_TIMEOUT_ZSET_KEY = "order:timeout:pending";
    private static final String SCHEDULER_LOCK_KEY = "lock:order:auto-cancel";
    private static final long SCHEDULER_LOCK_TTL_SECONDS = 60;

    /**
     * 每分钟扫描一次超时订单
     */
    @Scheduled(fixedRate = 60000) // 每分钟执行一次
    public void autoCancelTimeoutOrders() {
        // 分布式锁：多实例部署时，同一时刻只有一个实例执行扫描，避免重复取消
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(
                SCHEDULER_LOCK_KEY, "1", SCHEDULER_LOCK_TTL_SECONDS, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(locked)) {
            log.info("未获取到分布式锁，可能有其他实例正在执行，跳过本次");
            return;
        }
        try {
            log.info("开始执行订单超时取消定时任务...");
            long currentTime = System.currentTimeMillis();

            // 查询所有score <= 当前时间的订单（已过期的）
            Set<Object> expiredOrderIds = redisTemplate.opsForZSet()
                .rangeByScore(ORDER_TIMEOUT_ZSET_KEY, 0, currentTime);

            if (expiredOrderIds == null || expiredOrderIds.isEmpty()) {
                log.info("没有超时订单需要处理");
                return;
            }

            log.info("发现 {} 个超时订单，开始取消...", expiredOrderIds.size());

            for (Object orderIdObj : expiredOrderIds) {
                try {
                    Long orderId = Long.parseLong(orderIdObj.toString());
                    orderService.cancelExpireOrder(orderId);

                    redisTemplate.opsForZSet().remove(ORDER_TIMEOUT_ZSET_KEY, orderIdObj);

                    log.info("订单 {} 超时取消成功，档期已释放", orderId);
                } catch (NumberFormatException e) {
                    log.error("无效的订单ID: {}", orderIdObj);
                } catch (Exception e) {
                    log.error("订单 {} 超时取消失败", orderIdObj, e);
                }
            }

            log.info("订单超时取消定时任务执行完成");
        } catch (Exception e) {
            log.error("订单超时取消定时任务执行失败", e);
        } finally {
            // 释放分布式锁，供下一轮/其他实例使用
            redisTemplate.delete(SCHEDULER_LOCK_KEY);
        }
    }

    /**
     * 每天凌晨 2 点检查并自动完成超时订单
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void autoCompleteOrders() {
        log.info("开始执行订单自动完成定时任务...");
        try {
            orderService.autoCompleteExpiredOrders();
            log.info("订单自动完成定时任务执行成功");
        } catch (Exception e) {
            log.error("订单自动完成定时任务执行失败", e);
        }
    }
}
