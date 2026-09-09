package com.xhxi.photobooker.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xhxi.photobooker.entity.Message;
import com.xhxi.photobooker.mapper.MessageMapper;
import com.xhxi.photobooker.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessageServiceImpl implements MessageService {
    @Autowired
    private MessageMapper messageMapper;

    @Override
    public void saveMessage(Message message) {
        messageMapper.insert(message);
    }

    @Override
    public List<Message> getHistory(Long orderId, int page, int size) {
        IPage<Message> pageObj = new Page<>(page, size);
        QueryWrapper<Message> wrapper = new QueryWrapper<>();
        wrapper.eq("order_id", orderId).orderByAsc("create_time");
        IPage<Message> result = messageMapper.selectPage(pageObj, wrapper);
        return result.getRecords();
    }

    @Override
    public List<Message> getHistoryBySession(String sessionId, int page, int size) {
        IPage<Message> pageObj = new Page<>(page, size);
        QueryWrapper<Message> wrapper = new QueryWrapper<>();
        wrapper.eq("session_id", sessionId).orderByAsc("create_time");
        IPage<Message> result = messageMapper.selectPage(pageObj, wrapper);
        return result.getRecords();
    }

    @Override
    public List<Message> getHistoryByUser(Long senderId, Long receiverId, int page, int size) {
        IPage<Message> pageObj = new Page<>(page, size);
        QueryWrapper<Message> wrapper = new QueryWrapper<>();
        wrapper.eq("sender_id", senderId).eq("receiver_id", receiverId).orderByAsc("create_time");
        IPage<Message> result = messageMapper.selectPage(pageObj, wrapper);
        return result.getRecords();
    }

    @Override
    public void markAsDelivered(Long msgId) {
        UpdateWrapper<Message> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", msgId).set("is_delivered", 1);
        messageMapper.update(null, updateWrapper);
    }
} 