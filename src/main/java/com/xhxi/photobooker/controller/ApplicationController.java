package com.xhxi.photobooker.controller;

import com.xhxi.photobooker.entity.Application;
import com.xhxi.photobooker.entity.User;
import com.xhxi.photobooker.result.Result;
import com.xhxi.photobooker.service.ApplicationService;
import com.xhxi.photobooker.service.PhotographerService;
import com.xhxi.photobooker.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/application")
public class ApplicationController {
    @Autowired
    private ApplicationService applicationService;
    @Autowired
    private PhotographerService photographerService;
    @Autowired
    private UserService userService;

    // 用户提交申请
    @PostMapping
    public Result<Boolean> submit(@RequestBody Application application) {
        applicationService.submitApplication(application);
        return Result.success(true);
    }

    // 用户查看自己的申请状态
    @GetMapping("/user/{userId}")
    public Result<List<Application>> getByUser(@PathVariable Long userId) {
        return Result.success(applicationService.getApplicationsByUserId(userId));
    }

    // 管理员查看所有申请
    @GetMapping("/list")
    public Result<List<Application>> getAll() {
        return Result.success(applicationService.getAllApplications());
    }

    // 管理员审核申请
    @PutMapping("/{id}/review")
    @Transactional
    public Result<Boolean> review(@PathVariable Long id, @RequestParam String status, @RequestParam(required = false) String remark) {
        applicationService.reviewApplication(id, status, remark);

        return Result.success(true);
    }

    // 获取单个申请详情
    @GetMapping("/{id}")
    public Result<Application> getById(@PathVariable Long id) {
        return Result.success(applicationService.getApplicationById(id));
    }


} 