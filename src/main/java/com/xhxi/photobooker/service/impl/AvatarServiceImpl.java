package com.xhxi.photobooker.service.impl;

import com.xhxi.photobooker.entity.Avatar;
import com.xhxi.photobooker.mapper.AvatarMapper;
import com.xhxi.photobooker.service.AvatarService;
import com.xhxi.photobooker.mapper.UserMapper;
import com.xhxi.photobooker.entity.User;
import com.xhxi.photobooker.mapper.PhotographerMapper;
import com.xhxi.photobooker.entity.Photographer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AvatarServiceImpl implements AvatarService {
    @Autowired
    private AvatarMapper avatarMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private PhotographerMapper photographerMapper;

    @Override
    public List<Avatar> getByUserId(Long userId) {
        return avatarMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Avatar>().eq("user_id", userId));
    }

    @Override
    public void upload(Avatar avatar) {
        avatarMapper.insert(avatar);
        // 同步更新user表的avatar字段
        if (avatar.getUserId() != null && avatar.getUrl() != null) {
            User user = userMapper.selectById(avatar.getUserId());
            if (user != null) {
                user.setAvatar(avatar.getUrl());
                userMapper.updateById(user);
            }
            // 同步更新photographer表的avatar字段
            Photographer photographer = photographerMapper.selectByUserId(avatar.getUserId());
            if (photographer != null) {
                photographer.setAvatar(avatar.getUrl());
                photographerMapper.updateById(photographer);
            }
        }
    }

    @Override
    public void setCurrent(Long userId, Long avatarId) {
        // 先将该用户所有头像设为非当前
        com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<Avatar> wrapper = new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
        wrapper.eq("user_id", userId).set("is_current", 0);
        avatarMapper.update(null, wrapper);
        // 再将指定头像设为当前
        Avatar avatar = avatarMapper.selectById(avatarId);
        if (avatar != null) {
            avatar.setIsCurrent(1);
            avatarMapper.updateById(avatar);
            // 同步更新user表的avatar字段
            if (avatar.getUserId() != null && avatar.getUrl() != null) {
                User user = userMapper.selectById(avatar.getUserId());
                if (user != null) {
                    user.setAvatar(avatar.getUrl());
                    userMapper.updateById(user);
                }
                // 同步更新photographer表的avatar字段
                Photographer photographer = photographerMapper.selectByUserId(avatar.getUserId());
                if (photographer != null) {
                    photographer.setAvatar(avatar.getUrl());
                    photographerMapper.updateById(photographer);
                }
            }
        }
    }

    @Override
    public void delete(Long id) {
        avatarMapper.deleteById(id);
    }
} 