package com.xhxi.photobooker.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.Date;

@Data
@TableName("portfolio_comment")
public class PortfolioComment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long portfolioId;
    private Long userId;
    private String userName;
    private String content;
    private Long parentId;
    private String parentUserName;
    private Integer status;
    private String avatar;
    private Date createTime;
    private Date updateTime;
} 