package com.xhxi.photobooker;

import com.xhxi.photobooker.entity.Order;
import com.xhxi.photobooker.enums.OrderStatus;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OrderStatusTest {

    @Test
    void testFromCode() {
        // 测试正常转换
        assertEquals(OrderStatus.PENDING, OrderStatus.fromCode("PENDING"));
        assertEquals(OrderStatus.COMPLETED, OrderStatus.fromCode("COMPLETED"));

        // 测试异常情况
        assertThrows(IllegalArgumentException.class, () -> {
            OrderStatus.fromCode("INVALID_STATUS");
        });
    }

    @Test
    void testDescription() {
        assertEquals("待支付", OrderStatus.PENDING.getDescription());
        assertEquals("已完成", OrderStatus.COMPLETED.getDescription());
    }
}

