package com.xhxi.photobooker.monitor;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class HeartbeatMonitor {
    
    private static final AtomicInteger totalHeartbeats = new AtomicInteger(0);
    private static final AtomicInteger timeoutConnections = new AtomicInteger(0);
    private static final AtomicLong lastHeartbeatTime = new AtomicLong(0);
    
    /**
     * 记录心跳
     */
    public void recordHeartbeat() {
        totalHeartbeats.incrementAndGet();
        lastHeartbeatTime.set(System.currentTimeMillis());
    }
    
    /**
     * 记录超时连接
     */
    public void recordTimeout() {
        timeoutConnections.incrementAndGet();
    }
    
    /**
     * 获取总心跳数
     */
    public int getTotalHeartbeats() {
        return totalHeartbeats.get();
    }
    
    /**
     * 获取超时连接数
     */
    public int getTimeoutConnections() {
        return timeoutConnections.get();
    }
    
    /**
     * 获取最后心跳时间
     */
    public long getLastHeartbeatTime() {
        return lastHeartbeatTime.get();
    }
    
    /**
     * 每分钟打印心跳统计信息
     */
    @Scheduled(fixedRate = 60000)
    public void printHeartbeatStats() {
        System.out.println("=== 心跳统计信息 ===");
        System.out.println("总心跳数: " + totalHeartbeats.get());
        System.out.println("超时连接数: " + timeoutConnections.get());
        System.out.println("最后心跳时间: " + new java.util.Date(lastHeartbeatTime.get()));
        System.out.println("==================");
    }
    
    /**
     * 重置统计信息
     */
    public void resetStats() {
        totalHeartbeats.set(0);
        timeoutConnections.set(0);
        lastHeartbeatTime.set(0);
    }
} 