// 新建文件：src/main/java/com/xhxi/photobooker/entity/AfterSalesService.java
package com.xhxi.photobooker.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("after_sales_service")
public class AfterSalesService implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;
    private Long userId;
    private Long photographerId;

    private String serviceType;
    private String status;

    // JSON 字段，存储灵活的服务数据
    @TableField(typeHandler = JacksonTypeHandler.class)
    private ServiceData serviceData;

    // 精修照片相关
    private String photoNumbers;
    private String refineRequirements;

    // 重新预约相关
    private LocalDateTime expectedTime;
    private String rescheduleReason;

    // 退款相关
    private String refundReason;
    private BigDecimal refundAmount;

    // 投诉建议相关
    private String complaintType;

    // 加选服务相关
    @TableField(typeHandler = JacksonTypeHandler.class)
    private AdditionalItems additionalItems;
    private BigDecimal additionalPrice;

    // 通用字段
    private String description;
    private String contactPhone;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private String[] evidenceImages;

    // 处理记录
    private Long handlerId;
    private String handlerResponse;
    private String processNotes;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime handleTime;
}
