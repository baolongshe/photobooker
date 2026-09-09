package com.xhxi.photobooker.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * ReAct Agent（原生工具调用循环）配置。
 * <p>
 * 对应 application.yml 中的 {@code agent.react.*} 配置段。
 */
@Data
@Component
@ConfigurationProperties(prefix = "agent.react")
public class ReActProperties {

    /** 是否启用 ReAct 工具调用循环 */
    private boolean enabled = true;

    /** 最大推理-行动轮数，防止无限循环 */
    private int maxIterations = 6;

    /** 同一工具调用失败后允许模型原样重试的最大次数 */
    private int maxSameCallRetries = 1;
}
