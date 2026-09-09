package com.xhxi.photobooker.agent;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 跨线程的函数调用上下文。
 * <p>
 * Spring AI 的 Function Calling 在 HTTP 连接池线程上执行函数回调，
 * 而用户认证信息（BaseContext）存储在当前请求线程的 ThreadLocal 中。
 * 这个类充当桥梁，让 HTTP 工作线程能获取到当前请求的用户信息。
 */
public final class FunctionCallContext {

    private static final AtomicReference<Long> currentUserId = new AtomicReference<>(null);

    private FunctionCallContext() {}

    public static void setUserId(Long userId) {
        currentUserId.set(userId);
    }

    public static Long getUserId() {
        return currentUserId.get();
    }

    public static void clear() {
        currentUserId.set(null);
    }
}
