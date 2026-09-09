package com.xhxi.photobooker.service.impl;

import com.xhxi.photobooker.entity.Announcement;
import com.xhxi.photobooker.mapper.AnnouncementMapper;
import com.xhxi.photobooker.service.AnnouncementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnnouncementServiceImpl implements AnnouncementService {
    @Autowired
    private AnnouncementMapper announcementMapper;

    @Override
    public List<Announcement> getAll() {
        return announcementMapper.selectList(null);
    }

    @Override
    public Announcement getLatest() {
        List<Announcement> list = announcementMapper.selectList(null);
        if (list == null || list.isEmpty()) return null;
        Announcement latest = list.get(0);
        for (Announcement a : list) {
            if (a.getTime() != null && latest.getTime() != null && a.getTime().after(latest.getTime())) {
                latest = a;
            }
        }
        return latest;
    }

    @Override
    public void add(Announcement announcement) {
        announcementMapper.insert(announcement);
    }

    @Override
    public void delete(Long id) {
        announcementMapper.deleteById(id);
    }

    @Override
    public void update(Announcement announcement) {
        announcementMapper.updateById(announcement);
    }
} 