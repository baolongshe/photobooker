package com.xhxi.photobooker.entity;

import lombok.Data;
import java.util.Date;

@Data
public class Application {
    private Long id;              // 主键
    private Long userId;          // 申请人用户ID
    private String realName;      // 真实姓名
    private String phone;         // 联系电话
    private String intro;         // 个人简介/申请理由
    private String status;        // 状态: PENDING(待审核), APPROVED(通过), REJECTED(拒绝)
    private String remark;        // 管理员审核备注
    private Date createTime;      // 申请时间
    private Date updateTime;      // 审核时间/更新时间
}
