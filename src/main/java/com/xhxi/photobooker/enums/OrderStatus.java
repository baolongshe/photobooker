package com.xhxi.photobooker.enums;

import lombok.Getter;

@Getter
public enum OrderStatus {
    PENDING("PENDING", "待支付"),
    CONFIRMED("CONFIRMED", "已确认"),
    IN_PROGRESS("IN_PROGRESS", "拍摄中"),
    PENDING_UPLOAD("PENDING_UPLOAD", "待上传照片"),
    PENDING_CONFIRM("PENDING_CONFIRM", "待确认完成"),
    COMPLETED("COMPLETED", "已完成"),
    CANCELED("CANCELED", "已取消"),
    FAILED("FAILED", "已失败"),
    TRADE_SUCCESS("TRADE_SUCCESS","支付成功"),
    REFUNDING("REFUNDING", "退款中"),
    REFUNDED("REFUNDED", "已退款"),
    REJECTED("REJECTED", "摄影师拒绝");

    private final String code;
    private final String description;

    OrderStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    // 根据 code 获取枚举
    public static OrderStatus fromCode(String code) {
        for (OrderStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("无效的订单状态码: " + code);
    }
}