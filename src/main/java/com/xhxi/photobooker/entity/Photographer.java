package com.xhxi.photobooker.entity;


import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.util.Date;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Photographer {
    @TableId("id")
    private Long id;
    @TableField("user_id")
    private Long userId;
    // 摄影师姓名
    private String name;
    // 联系电话
    private String phone;
    // 擅长风格（如：婚纱、写真、纪实，可拼接或用枚举）
    private String style;
    // 简介
    private String intro;
    // 工作年限
    private Integer workYears;
    // 头像地址（存储文件路径或OSS链接）
    private String avatar;
    // 认证状态（0：未认证，1：已认证，2：认证失败）
    private Integer authStatus;
    // 可用档期（可存JSON或拆分关联表，简单场景用字符串拼接）
    private String availableSchedule;
    // 评分
    private Double rating;
    // 完成订单数
    private Integer orderCount;
    // 地址
    private String location;
    // 纬度
    private Double latitude;
    // 经度
    private Double longitude;
    // 套餐信息（JSON格式存储）
    private String packages;
    // 创建时间
    private Date createTime;
    // 更新时间
    private Date updateTime;
    // 位置是否公开（1公开，0隐藏）
    private Integer locationVisible;
    // 最后定位上传时间
    private Date lastLocationUpdateTime;
    // 是否在工作时间
    private int working;
}
