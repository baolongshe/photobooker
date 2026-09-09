package com.xhxi.photobooker.service;

import com.xhxi.photobooker.entity.Avatar;
import java.util.List;

public interface AvatarService {
    List<Avatar> getByUserId(Long userId);
    void upload(Avatar avatar);
    void setCurrent(Long userId, Long avatarId);
    void delete(Long id);
} 