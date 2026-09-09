package com.xhxi.photobooker.entity;

import lombok.Data;

@Data
public class Admin {
    // 主键，自增
    private Long id;
    // 管理员账号
    private String username;
    // 加密后的密码
    private String password;
    // 真实姓名
    private String realName;
    // 联系电话
    private String phone;
    // 角色（如：超级管理员、普通管理员，可扩展枚举）
    private String role;
    // 是否启用（0：禁用，1：启用）
    private Integer status;
    // 关联user表id
    private Long userId;
}
