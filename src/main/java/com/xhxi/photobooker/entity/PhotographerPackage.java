package com.xhxi.photobooker.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 摄影师套餐实体类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("photographer_package")
public class PhotographerPackage {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 摄影师ID
     */
    @TableField("photographer_id")
    private Long photographerId;
    
    /**
     * 套餐名称
     */
    private String name;
    
    /**
     * 套餐描述
     */
    private String description;
    
    /**
     * 套餐价格
     */
    private BigDecimal price;
    
    /**
     * 拍摄时长（小时）
     */
    private Integer duration;
    
    /**
     * 精修照片数量
     */
    @TableField("photo_count")
    private Integer photoCount;
    
    /**
     * 服务详情
     */
    @TableField("service_details")
    private String serviceDetails;
    
    /**
     * 套餐分类
     */
    private String category;
    
    /**
     * 状态（0：下架，1：上架）
     */
    private Integer status;
    
    /**
     * 是否默认套餐（0：否，1：是）
     */
    @TableField("is_default")
    private Integer isDefault;
    
    /**
     * 创建时间
     */
    @TableField("create_time")
    private Date createTime;
    
    /**
     * 更新时间
     */
    @TableField("update_time")
    private Date updateTime;
}
