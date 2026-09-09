package com.xhxi.photobooker.entity;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User implements Serializable {
    private Long id;
    // 用户名（或手机号/邮箱，根据登录逻辑调整）
    private String username;
    // 加密密码
    private String password;
    // 真实姓名
    private String realName;
    // 联系电话
    private String phone;
    // 性别（0：女，1：男，2：其他）
    private Integer gender;
    // 生日
    private LocalDate birthday;
    // 头像地址
    private String avatar;
    // 注册时间
    private LocalDateTime registerTime;
    // 最后登录时间
    private LocalDateTime lastLoginTime;
    // 状态（0：禁用，1：正常）
    private Integer status;
    //微信登陆唯一标识
    private String openid;
    //角色(0:普通用户 1：管理员 )
    private Integer role;
}
