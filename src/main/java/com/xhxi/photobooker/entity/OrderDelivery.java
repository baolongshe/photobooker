package com.xhxi.photobooker.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("order_delivery")
public class OrderDelivery implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;
    private Long photographerId;
    private String deliveryBatch;
    private Integer totalCount;
    private Integer deliveredCount;
    private String status;
    private LocalDateTime deliveryTime;
    private String remark;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
