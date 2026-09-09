package com.xhxi.photobooker.controller;

import com.xhxi.photobooker.entity.Message;
import com.xhxi.photobooker.result.Result;
import com.xhxi.photobooker.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

import java.util.List;

@RestController
@RequestMapping("/chat")
public class    ChatController {
    @Autowired
    private MessageService messageService;

    // 聊天消息存储
    @PostMapping("/message")
    public Result<Boolean> saveMessage(@RequestBody Message message) {
        messageService.saveMessage(message);
        return Result.success(true);
    }

    // 聊天历史查询（分页）
    @GetMapping("/history")
    public Result<List<Message>> getHistory(@RequestParam(required = false) Long orderId,
                                            @RequestParam(required = false) String sessionId,
                                            @RequestParam(required = false) Long senderId,
                                            @RequestParam(required = false) Long receiverId,
                                            @RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        List<Message> history;
        if (sessionId != null) {
            history = messageService.getHistoryBySession(sessionId, page, size);
        } else if (senderId != null && receiverId != null) {
            history = messageService.getHistoryByUser(senderId, receiverId, page, size);
        } else if (orderId != null) {
            history = messageService.getHistory(orderId, page, size);
        } else {
            history = List.of();
        }
        return Result.success(history);
    }

    // 富媒体上传接口
    @PostMapping("/upload")
    public Result<String> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        // 1. 保存到本地（可替换为云存储）
        String uploadDir = "uploads/";
        File dir = new File(uploadDir);
        if (!dir.exists()) dir.mkdirs();
        String ext = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf('.'));
        String filename = UUID.randomUUID() + ext;
        File dest = new File(dir, filename);
        file.transferTo(dest);
        // 2. 返回可访问URL（假设静态资源映射到/upload s）
        String url = "/uploads/" + filename;
        return Result.success(url);
    }
}