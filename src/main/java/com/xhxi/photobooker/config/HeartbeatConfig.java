package com.xhxi.photobooker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "websocket.heartbeat")
public class HeartbeatConfig {
    
    /**
     * 心跳间隔（秒）
     */
    private int interval = 30;
    
    /**
     * 心跳超时时间（秒）
     */
    private int timeout = 60;
    
    /**
     * 心跳检查间隔（秒）
     */
    private int checkInterval = 30;
    
    public int getInterval() {
        return interval;
    }
    
    public void setInterval(int interval) {
        this.interval = interval;
    }
    
    public int getTimeout() {
        return timeout;
    }
    
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
    
    public int getCheckInterval() {
        return checkInterval;
    }
    
    public void setCheckInterval(int checkInterval) {
        this.checkInterval = checkInterval;
    }
} 