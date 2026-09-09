package com.xhxi.photobooker.controller;

import com.xhxi.photobooker.entity.Announcement;
import com.xhxi.photobooker.result.Result;
import com.xhxi.photobooker.service.AnnouncementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/announcement")
public class AnnouncementController {
    private static final Logger logger = LoggerFactory.getLogger(AnnouncementController.class);
    
    @Autowired
    private AnnouncementService announcementService;

    /**
     * 获取所有公告
     */
    @GetMapping("/list")
    public Result<List<Announcement>> getAll() {
        logger.info("获取所有公告");
        try {
            List<Announcement> announcements = announcementService.getAll();
            logger.info("获取公告列表成功，数量={}", announcements.size());
            return Result.success(announcements);
        } catch (Exception e) {
            logger.error("获取公告列表失败", e);
            return Result.error("获取公告列表失败，请稍后重试");
        }
    }

    /**
     * 获取最新公告
     */
    @GetMapping("/latest")
    public Result<Announcement> getLatest() {
        logger.info("获取最新公告");
        try {
            Announcement announcement = announcementService.getLatest();
            logger.info("获取最新公告成功");
            return Result.success(announcement);
        } catch (Exception e) {
            logger.error("获取最新公告失败", e);
            return Result.error("获取最新公告失败，请稍后重试");
        }
    }

    /**
     * 添加新公告
     */
    @PostMapping("/add")
    public Result<Boolean> add(@RequestBody Announcement announcement) {
        logger.info("添加新公告: {}", announcement);
        try {
            if (announcement == null) {
                return Result.error("公告内容不能为空");
            }
            if (announcement.getTitle() == null || announcement.getTitle().trim().isEmpty()) {
                return Result.error("公告标题不能为空");
            }
            if (announcement.getContent() == null || announcement.getContent().trim().isEmpty()) {
                return Result.error("公告内容不能为空");
            }
            announcementService.add(announcement);
            logger.info("公告添加成功: id={}", announcement.getId());
            return Result.success(true);
        } catch (Exception e) {
            logger.error("添加公告失败", e);
            return Result.error("添加公告失败，请稍后重试");
        }
    }

    /**
     * 删除公告
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        logger.info("删除公告: id={}", id);
        try {
            if (id == null || id <= 0) {
                return Result.error("公告ID无效");
            }
            announcementService.delete(id);
            logger.info("公告删除成功: id={}", id);
            return Result.success(true);
        } catch (Exception e) {
            logger.error("删除公告失败: id={}", id, e);
            return Result.error("删除公告失败，请稍后重试");
        }
    }

    /**
     * 更新公告
     */
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody Announcement announcement) {
        logger.info("更新公告: id={}, {}", id, announcement);
        try {
            if (id == null || id <= 0) {
                return Result.error("公告ID无效");
            }
            if (announcement == null) {
                return Result.error("公告内容不能为空");
            }
            if (announcement.getTitle() == null || announcement.getTitle().trim().isEmpty()) {
                return Result.error("公告标题不能为空");
            }
            if (announcement.getContent() == null || announcement.getContent().trim().isEmpty()) {
                return Result.error("公告内容不能为空");
            }
            announcement.setId(id);
            announcementService.update(announcement);
            logger.info("公告更新成功: id={}", id);
            return Result.success(true);
        } catch (Exception e) {
            logger.error("更新公告失败: id={}", id, e);
            return Result.error("更新公告失败，请稍后重试");
        }
    }
}