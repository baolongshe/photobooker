// 新建文件：D:\JavaWork\PhotoBooker\PhotoBooker\src\main\java\com\xhxi\photobooker\entity\OrderPhoto.java
package com.xhxi.photobooker.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName(value = "order_photo", autoResultMap = true)
public class OrderPhoto implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long deliveryId;
    private Long orderId;
    private String photoNumber;
    private String photoUrl;
    private String thumbnailUrl;
    private Integer isSelected;
    private Integer isDelivered;
    private Long fileSize;
    private Integer width;
    private Integer height;
    private String description;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private String[] tags;

    private Integer sortOrder;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
