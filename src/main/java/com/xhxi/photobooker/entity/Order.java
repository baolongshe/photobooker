package com.xhxi.photobooker.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import com.xhxi.photobooker.enums.OrderStatus;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
@TableName("booking_order")
public class Order implements Serializable {
        @TableId(type = IdType.AUTO)
        private Long id;
        private Long userId;
        private Long photographerId;
        @TableField(exist = false)
        private String photographerName;
        private String packageName;
        private BigDecimal totalPrice;
        private LocalDateTime shootingTime;
        private String shootingLocation;
        private OrderStatus status;
        private LocalDateTime createTime;
        private LocalDateTime updateTime;
        @TableField(exist = false)
        private String userName;
        @TableField(exist = false)
        private String userAvatar;
        @TableField(exist = false)
        private java.util.List<String> deliveredPhotos;
}
