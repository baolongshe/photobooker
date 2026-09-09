package com.xhxi.photobooker.entity;

import lombok.Data;
import java.util.Date;

@Data
public class Avatar {
    private Long id;
    private Long userId;
    private String url;
    private Integer isCurrent; // 1=当前头像，0=历史头像
    private Date uploadTime;
    private Integer status; // 1=正常，0=禁用/待审核等
} 