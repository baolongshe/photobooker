package com.xhxi.photobooker.controller;

import com.xhxi.photobooker.entity.Avatar;
import com.xhxi.photobooker.result.Result;
import com.xhxi.photobooker.service.AvatarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/avatar")
public class AvatarController {
    @Autowired
    private AvatarService avatarService;

    @GetMapping("/list")
    public Result<List<Avatar>> list(@RequestParam Long userId) {
        return Result.success(avatarService.getByUserId(userId));
    }

    @PostMapping("/upload")
    public Result<Boolean> upload(@RequestBody Avatar avatar) {
        avatarService.upload(avatar);
        return Result.success(true);
    }

    @PutMapping("/{id}/setCurrent")
    public Result<Boolean> setCurrent(@RequestParam Long userId, @PathVariable Long id) {
        avatarService.setCurrent(userId, id);
        return Result.success(true);
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        avatarService.delete(id);
        return Result.success(true);
    }
} 