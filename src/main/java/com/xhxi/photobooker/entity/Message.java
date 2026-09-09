package com.xhxi.photobooker.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import java.util.Date;

@Data
public class Message {
    private Long id;              // 主键
    private String sessionId;     // 会话ID（可选）
    private Long senderId;        // 发送者ID
    private String senderRole;    // 发送者角色
    private Long receiverId;      // 接收者ID
    private String receiverRole;  // 接收者角色
    private Long orderId;         // 关联订单ID
    private String content;       // 文本内容
    private String type;          // 消息类型：text/image/file/login/chat/ack
    private String fileUrl;       // 图片/文件URL
    private Date createTime;      // 发送时间
    private Integer isDelivered;  // 是否已送达：0-未送达，1-已送达

    // 为了兼容Netty处理，添加别名方法
    public Long getFromUserId() {
        return this.senderId;
    }
    
    public Long getToUserId() {
        return this.receiverId;
    }
    
    public Long getMsgId() {
        return this.id;
    }

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public byte[] toJson() {
        try {
            return objectMapper.writeValueAsBytes(this);
        } catch (Exception e) {
            // 实际项目建议更优雅的异常处理（如打日志、抛自定义异常）
            e.printStackTrace();
            return new byte[0];
        }
    }
}