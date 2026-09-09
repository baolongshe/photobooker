package com.xhxi.photobooker.service;

import com.xhxi.photobooker.entity.Announcement;
import java.util.List;

public interface AnnouncementService {
    List<Announcement> getAll();
    Announcement getLatest();
    void add(Announcement announcement);
    void delete(Long id);
    void update(Announcement announcement);
} 