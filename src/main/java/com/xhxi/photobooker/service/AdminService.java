package com.xhxi.photobooker.service;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xhxi.photobooker.entity.Admin;
import com.xhxi.photobooker.entity.User;
import org.springframework.stereotype.Service;

@Service
public interface AdminService  {
    Admin getByUsername(String username);
    boolean getById(Long id);
    /*修改密码*/
    void updatePassword(User user);
}