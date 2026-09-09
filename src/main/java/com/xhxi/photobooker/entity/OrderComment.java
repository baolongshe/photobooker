package com.xhxi.photobooker.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName(value = "order_comment", autoResultMap = true)
public class OrderComment implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;
    private Long userId;
    private Long photographerId;

    private Integer rating;
    private String content;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private String[] images;

    private Integer isAnonymous;
    private Integer status;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
