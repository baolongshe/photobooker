package com.xhxi.photobooker.enums;

import lombok.Getter;

/**
 * 售后服务状态枚举
 */
@Getter
public enum ServiceStatus {
    PENDING("PENDING", "待处理"),
    APPROVED("APPROVED", "已批准"),
    REJECTED("REJECTED", "已拒绝"),
    PROCESSING("PROCESSING", "处理中"),
    COMPLETED("COMPLETED", "已完成"),
    CANCELLED("CANCELLED", "已撤销");

    private final String code;
    private final String description;

    ServiceStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    // 根据 code 获取枚举
    public static ServiceStatus fromCode(String code) {
        for (ServiceStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("无效的售后服务状态码: " + code);
    }
}
