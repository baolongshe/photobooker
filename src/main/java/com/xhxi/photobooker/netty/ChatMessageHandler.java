package com.xhxi.photobooker.netty;
import com.alibaba.fastjson.JSON;
import com.xhxi.photobooker.entity.Message;
import com.xhxi.photobooker.service.MessageService;
import com.xhxi.photobooker.utils.JwtUtil;
import com.xhxi.photobooker.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.netty.channel.*;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.CharsetUtil;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Date;

public class ChatMessageHandler extends SimpleChannelInboundHandler<Object> {
    private static final ConcurrentHashMap<Long, Channel> userChannelMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, String> userNameCache = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService heartbeatExecutor = Executors.newScheduledThreadPool(1);
    private static final int HEARTBEAT_TIMEOUT = 60; // 60秒超时
    private static final AtomicInteger activeConnections = new AtomicInteger(0);
    private WebSocketServerHandshaker handshaker;
    private boolean isWebSocketHandshakeComplete = false;

    private MessageService getMessageService() {
        return SpringContextUtils.getBean(MessageService.class);
    }

    private JwtProperties getJwtProperties() {
        return SpringContextUtils.getBean(JwtProperties.class);
    }
    
    // 启动心跳检查
    static {
        startHeartbeatCheck();
    }
    
    private static void startHeartbeatCheck() {
        heartbeatExecutor.scheduleAtFixedRate(() -> {
            checkHeartbeat();
        }, 30, 30, TimeUnit.SECONDS); // 每30秒检查一次

    }
    
    private static void checkHeartbeat() {
        long currentTime = System.currentTimeMillis();
        final int[] timeoutCount = {0};
        
        userChannelMap.forEach((userId, channel) -> {
            if (channel.isActive()) {
                Object lastHeartbeatObj = channel.attr(io.netty.util.AttributeKey.valueOf("lastHeartbeat")).get();
                Long lastHeartbeat = lastHeartbeatObj != null ? (Long) lastHeartbeatObj : null;
                
                if (lastHeartbeat != null && 
                    currentTime - lastHeartbeat > HEARTBEAT_TIMEOUT * 1000) {
                    System.out.println("用户 " + userId + " 心跳超时，关闭连接");
                    channel.close();
                    userChannelMap.remove(userId);
                    timeoutCount[0]++;
                }
            } else {
                // 连接已断开，清理资源
                userChannelMap.remove(userId);
            }
        });
        
        if (timeoutCount[0] > 0) {
            System.out.println("清理了 " + timeoutCount[0] + " 个超时连接");
        }
        System.out.println("当前活跃连接数: " + userChannelMap.size());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("收到消息类型: " + msg.getClass().getSimpleName() + ", 握手状态: " + isWebSocketHandshakeComplete);
        
        if (!isWebSocketHandshakeComplete && msg instanceof FullHttpRequest) {
            System.out.println("处理HTTP握手请求");
            handleHttpRequest(ctx, (FullHttpRequest) msg);
        } else if (isWebSocketHandshakeComplete && msg instanceof WebSocketFrame) {
            System.out.println("处理WebSocket消息");
            handleWebSocketFrame(ctx, (WebSocketFrame) msg);
        } else if (msg instanceof WebSocketFrame && !isWebSocketHandshakeComplete) {
            System.out.println("收到WebSocket消息但握手未完成，忽略");
        } else {
            System.out.println("忽略未知类型的消息: " + msg.getClass().getSimpleName());
        }
    }

    private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest request) {
        System.out.println("开始处理WebSocket握手请求");
        
        // 处理WebSocket握手请求
        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
                "ws://localhost:8081/ws", null, false);
        handshaker = wsFactory.newHandshaker(request);
        
        if (handshaker == null) {
            System.err.println("WebSocket握手失败，不支持的版本");
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            // 验证token
            String token = extractToken(request);
            if (token != null && validateToken(token)) {
                try {
                    Claims claims = JwtUtil.parseJWT(getJwtProperties().getUserSecretKey(), token);
                    Long userId = claims.get("userId", Long.class);
                    if (userId != null) {
                        userChannelMap.put(userId, ctx.channel());
                        // 记录连接建立时间和初始心跳时间
                        ctx.channel().attr(io.netty.util.AttributeKey.valueOf("lastHeartbeat"))
                            .set(System.currentTimeMillis());
                        activeConnections.incrementAndGet();
                        
                        handshaker.handshake(ctx.channel(), request);
                        isWebSocketHandshakeComplete = true;
                        System.out.println("用户 " + userId + " WebSocket连接建立成功");
                    } else {
                        System.err.println("Token中未找到userId");
                        ctx.close();
                    }
                } catch (Exception e) {
                    System.err.println("Token解析失败: " + e.getMessage());
                    ctx.close();
                }
            } else {
                System.err.println("Token验证失败");
                ctx.close();
            }
        }
    }

    private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
        System.out.println("处理WebSocket帧frame: " + frame.getClass().getSimpleName());


        if (frame instanceof TextWebSocketFrame) {
            String json = ((TextWebSocketFrame) frame).text();
            System.out.println("收到WebSocket文本消息: " + json);
            
            try {
                // 先解析为JSONObject，然后手动映射字段
                com.alibaba.fastjson.JSONObject jsonObj = JSON.parseObject(json);
                Message chatMsg = new Message();
                
                // 手动映射字段，处理前端和后端字段名不一致的问题
                chatMsg.setType(jsonObj.getString("type"));
                chatMsg.setSenderId(jsonObj.getLong("fromUserId"));  // 前端用fromUserId，后端用senderId
                chatMsg.setReceiverId(jsonObj.getLong("toUserId"));  // 前端用toUserId，后端用receiverId
                chatMsg.setOrderId(jsonObj.getLong("orderId"));
                chatMsg.setSessionId(jsonObj.getString("sessionId"));
                chatMsg.setContent(jsonObj.getString("content"));
                chatMsg.setFileUrl(jsonObj.getString("fileUrl"));
                chatMsg.setCreateTime(new Date());
                chatMsg.setIsDelivered(0); // 默认未送达
                
                // 设置角色信息（如果没有的话）
                if (jsonObj.getString("senderRole") != null) {
                    chatMsg.setSenderRole(jsonObj.getString("senderRole"));
                } else {
                    chatMsg.setSenderRole("user"); // 默认角色
                }
                if (jsonObj.getString("receiverRole") != null) {
                    chatMsg.setReceiverRole(jsonObj.getString("receiverRole"));
                } else {
                    chatMsg.setReceiverRole("user"); // 默认角色
                }
                
                System.out.println("解析后的消息对象: " + chatMsg);

                if ("ping".equals(chatMsg.getType())) {
                    // 处理心跳消息
                    handlePingMessage(ctx, jsonObj);
                } else if ("pong".equals(chatMsg.getType())) {
                    // 处理pong响应
                    handlePongMessage(ctx, jsonObj);
                } else if ("login".equals(chatMsg.getType())) {
                    userChannelMap.put(chatMsg.getFromUserId(), ctx.channel());
                    System.out.println("用户 " + chatMsg.getFromUserId() + " 登录成功");
                } else if ("chat".equals(chatMsg.getType()) || "text".equals(chatMsg.getType())) {
                    // 生成基于用户的会话ID：chat_{minUserId}_{maxUserId}
                    // 确保同一对用户的所有订单聊天记录都在同一个会话中
                    String sessionId = generateUserSessionId(chatMsg.getSenderId(), chatMsg.getReceiverId());
                    chatMsg.setSessionId(sessionId);
                    
                    // 保存到数据库
                    getMessageService().saveMessage(chatMsg);
                        
                    // 转发给接收者
                    Channel toChannel = userChannelMap.get(chatMsg.getToUserId());
                    if (toChannel != null && toChannel.isActive()) {
                        // 检查接收方是否是发送方自己
                        Channel fromChannel = userChannelMap.get(chatMsg.getSenderId());
                        if (toChannel.equals(fromChannel)) {
                            System.out.println("跳过转发：接收方是发送方自己，用户 " + chatMsg.getToUserId());
                            return;
                        }
                            
                        // 查询发送者的真实用户名
                        String senderName = getUserNameById(chatMsg.getSenderId());
                        
                        com.alibaba.fastjson.JSONObject forwardMsg = new com.alibaba.fastjson.JSONObject();
                        forwardMsg.put("fromUserId", chatMsg.getSenderId());
                        forwardMsg.put("toUserId", chatMsg.getToUserId());
                        forwardMsg.put("fromName", senderName);
                        forwardMsg.put("message", chatMsg.getContent());
                        forwardMsg.put("type", chatMsg.getType());
                        forwardMsg.put("fileUrl", chatMsg.getFileUrl());
                        forwardMsg.put("timestamp", chatMsg.getCreateTime());
                        forwardMsg.put("sessionId", sessionId);
                            
                        toChannel.writeAndFlush(new TextWebSocketFrame(forwardMsg.toJSONString()));
                        System.out.println("消息已转发给用户 " + chatMsg.getToUserId());
                    } else {
                        System.out.println("用户 " + chatMsg.getToUserId() + " 不在线，消息无法转发");
                    }
                } else if ("ack".equals(chatMsg.getType())) {
                    getMessageService().markAsDelivered(chatMsg.getMsgId());
                    System.out.println("消息 " + chatMsg.getMsgId() + " 已标记为送达");
                } else {
                    System.out.println("未知的消息类型: " + chatMsg.getType());
                }
            } catch (Exception e) {
                System.err.println("处理WebSocket消息时出错: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("收到非文本WebSocket帧: " + frame.getClass().getSimpleName());
        }
    }

    private String extractToken(FullHttpRequest request) {
        String uri = request.uri();
        System.out.println("请求URI: " + uri);
        
        if (uri.contains("token=")) {
            String[] params = uri.split("\\?")[1].split("&");
            for (String param : params) {
                if (param.startsWith("token=")) {
                    String token = param.substring(6);
                    System.out.println("提取到token: " + token.substring(0, Math.min(20, token.length())) + "...");
                    return token;
                }
            }
        }
        System.err.println("未找到token参数");
        return null;
    }

    private boolean validateToken(String token) {
        try {
            JwtUtil.parseJWT(getJwtProperties().getUserSecretKey(), token);
            return true;
        } catch (Exception e) {
            System.err.println("Token验证失败: " + e.getMessage());
            return false;
        }
    }
    
    // 处理心跳消息
    private void handlePingMessage(ChannelHandlerContext ctx, com.alibaba.fastjson.JSONObject pingMsg) {
        // 更新心跳时间
        ctx.channel().attr(io.netty.util.AttributeKey.valueOf("lastHeartbeat"))
            .set(System.currentTimeMillis());
        
        // 回复pong消息
        com.alibaba.fastjson.JSONObject pongMsg = new com.alibaba.fastjson.JSONObject();
        pongMsg.put("type", "pong");
        pongMsg.put("timestamp", System.currentTimeMillis());
        
        ctx.channel().writeAndFlush(new TextWebSocketFrame(pongMsg.toJSONString()));
        System.out.println("收到心跳，回复pong");
    }
    
    // 处理pong响应
    private void handlePongMessage(ChannelHandlerContext ctx, com.alibaba.fastjson.JSONObject pongMsg) {
        // 更新心跳时间
        ctx.channel().attr(io.netty.util.AttributeKey.valueOf("lastHeartbeat"))
            .set(System.currentTimeMillis());
        System.out.println("收到pong响应");
    }

    /**
     * 根据用户ID获取用户名（带缓存）
     * 优先从user表查询，其次从photographer表查询
     */
    private String getUserNameById(Long userId) {
        if (userId == null) {
            return "未知用户";
        }
        
        // 先从缓存中查找
        if (userNameCache.containsKey(userId)) {
            return userNameCache.get(userId);
        }
        
        try {
            // 尝试从UserService查询用户
            com.xhxi.photobooker.service.UserService userService = 
                SpringContextUtils.getBean(com.xhxi.photobooker.service.UserService.class);
            com.xhxi.photobooker.entity.User user = userService.findUserById(userId);
            
            if (user != null && user.getUsername() != null && !user.getUsername().isEmpty()) {
                String username = user.getUsername();
                userNameCache.put(userId, username); // 缓存
                return username;
            }
            
            // 如果user表中没有，尝试从PhotographerService查询摄影师
            com.xhxi.photobooker.service.PhotographerService photographerService = 
                SpringContextUtils.getBean(com.xhxi.photobooker.service.PhotographerService.class);
            com.xhxi.photobooker.entity.Photographer photographer = photographerService.findByUserId(userId);
            
            if (photographer != null && photographer.getName() != null && !photographer.getName().isEmpty()) {
                String name = photographer.getName();
                userNameCache.put(userId, name); // 缓存
                return name;
            }
        } catch (Exception e) {
            System.err.println("获取用户名失败 (userId=" + userId + "): " + e.getMessage());
        }
        
        // fallback：返回带ID的默认名称
        String defaultName = "用户" + userId;
        userNameCache.put(userId, defaultName);
        return defaultName;
    }

    /**
     * 生成基于用户的会话ID
     * 格式：chat_{minUserId}_{maxUserId}
     * 确保同一对用户的所有订单聊天记录都在同一个会话中
     * 例如：chat_20_24 表示用户20和摄影师24的所有聊天记录
     */
    private String generateUserSessionId(Long senderId, Long receiverId) {
        if (senderId == null || receiverId == null) {
            return "chat_" + System.currentTimeMillis();
        }
        
        // 将两个用户ID排序，确保无论谁发消息，sessionId都一致
        Long minUserId = Math.min(senderId, receiverId);
        Long maxUserId = Math.max(senderId, receiverId);
        
        return "chat_" + minUserId + "_" + maxUserId;
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        // 从用户映射中移除当前连接
        userChannelMap.values().remove(ctx.channel());
        activeConnections.decrementAndGet();
        System.out.println("WebSocket连接已关闭，当前活跃连接数: " + activeConnections.get());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.err.println("WebSocket处理异常: " + cause.getMessage());
        System.err.println("异常类型: " + cause.getClass().getSimpleName());
        cause.printStackTrace();
        ctx.close();
    }
}