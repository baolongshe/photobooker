package com.xhxi.photobooker.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.xhxi.photobooker.entity.Admin;
import com.xhxi.photobooker.entity.User;
import com.xhxi.photobooker.mapper.AdminMapper;
import com.xhxi.photobooker.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

@Service
public class AdminServiceImpl implements AdminService {
    @Autowired
    private AdminMapper adminMapper;

    public Admin getByUsername(String username) {
        return adminMapper.getByUsername(username);
    }

    @Override
    public boolean getById(Long id) {
        return adminMapper.selectByUserId(id) > 0;
    }

    @Override

    public void updatePassword(User user) {
        UpdateWrapper<Admin> wrapper = new UpdateWrapper<>();
        wrapper.eq("user_id",user.getId()).set("password",user.getPassword());
        adminMapper.update(null,wrapper);
    }

}
