package com.xhxi.photobooker.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("portfolio_like")
public class PortfolioLike {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long portfolioId;
    private Date createTime;
    private Byte favoriteType;  // 1=作品，2=订单照片
    private Long orderPhotoId;  // 订单照片 ID
    private Long orderId;       // 订单ID
    private String photoUrl;    // 照片 URL
    private String thumbnailUrl; // 缩略图 URL
    private String photoNumber;  // 照片编号
}