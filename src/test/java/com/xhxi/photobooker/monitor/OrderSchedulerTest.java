package com.xhxi.photobooker.monitor;

import com.xhxi.photobooker.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 订单超时取消定时任务的分布式锁测试。
 * 多实例部署时，同一时刻只能有一个实例执行扫描，避免重复取消。
 */
@ExtendWith(MockitoExtension.class)
class OrderSchedulerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOps;

    @Mock
    private ZSetOperations<String, Object> zSetOps;

    @InjectMocks
    private OrderScheduler scheduler;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    void autoCancelTimeoutOrders_skipsWhenLockNotAcquired() {
        // 另一个实例已持有锁：setIfAbsent 返回 false
        when(valueOps.setIfAbsent(anyString(), any(), anyLong(), any())).thenReturn(false);

        scheduler.autoCancelTimeoutOrders();

        // 未抢到锁时，不应扫描 ZSet，也不应取消任何订单
        verify(zSetOps, never()).rangeByScore(anyString(), anyDouble(), anyDouble());
        verify(orderService, never()).cancelExpireOrder(anyLong());
    }

    @Test
    void autoCancelTimeoutOrders_cancelsExpiredOrdersAndReleasesLockWhenLockAcquired() {
        // 抢到锁，且有 2 个超时订单
        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
        when(valueOps.setIfAbsent(anyString(), any(), anyLong(), any())).thenReturn(true);
        when(zSetOps.rangeByScore(anyString(), anyDouble(), anyDouble()))
                .thenReturn(new LinkedHashSet<>(Arrays.asList("1", "2")));

        scheduler.autoCancelTimeoutOrders();

        verify(orderService).cancelExpireOrder(1L);
        verify(orderService).cancelExpireOrder(2L);
        verify(zSetOps, times(2)).remove(anyString(), any());
        // 执行完毕后释放分布式锁
        verify(redisTemplate).delete(anyString());
    }
}
