package com.xhxi.photobooker.service;

import com.xhxi.photobooker.entity.Message;
import java.util.List;

public interface MessageService {
    void saveMessage(Message message);
    List<Message> getHistory(Long orderId, int page, int size);
    List<Message> getHistoryBySession(String sessionId, int page, int size);
    List<Message> getHistoryByUser(Long senderId, Long receiverId, int page, int size);
    void markAsDelivered(Long msgId);
} 