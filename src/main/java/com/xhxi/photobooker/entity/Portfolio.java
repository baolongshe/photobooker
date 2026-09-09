package com.xhxi.photobooker.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 摄影师作品表
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("portfolio")
public class Portfolio {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 摄影师ID（关联photographer表的id）
     */
    @TableField("photographer_id")
    private Long photographerId;
    
    /**
     * 作品标题
     */
    private String title;
    
    /**
     * 作品描述
     */
    private String description;
    
    /**
     * 作品类型（如：婚纱、写真、纪实、商业等）
     */
    private String category;
    
    /**
     * 作品标签（多个标签用逗号分隔）
     */
    private String tags;
    
    /**
     * 封面图片URL
     */
    @TableField("cover_image")
    private String coverImage;
    
    /**
     * 作品图片URLs（JSON数组格式存储多张图片）
     */
    @TableField("image_urls")
    private String imageUrls;
    
    /**
     * 拍摄时间
     */
    @TableField("shooting_date")
    private Date shootingDate;
    
    /**
     * 拍摄地点
     */
    @TableField("shooting_location")
    private String shootingLocation;
    
    /**
     * 使用的设备信息
     */
    private String equipment;
    
    /**
     * 作品状态（0：草稿，1：已发布，2：已下架）
     */
    private Integer status;
    
    /**
     * 浏览量
     */
    @TableField("view_count")
    private Integer viewCount;
    
    /**
     * 点赞数
     */
    @TableField("like_count")
    private Integer likeCount;
    
    /**
     * 是否设为精选作品（0：否，1：是）
     */
    @TableField("is_featured")
    private Integer isFeatured;
    
    /**
     * 排序权重（数字越大越靠前）
     */
    @TableField("sort_weight")
    private Integer sortWeight;
    
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