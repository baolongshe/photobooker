package com.xhxi.photobooker.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/test")
public class TestController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello, 应用正常运行！";
    }

    @GetMapping("/info")
    public Map<String, Object> info() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("message", "应用正常运行");
        result.put("timestamp", System.currentTimeMillis());
        result.put("port", 8099);
        return result;
    }

    @GetMapping("/pay-test")
    public String payTest() {
        return "支付测试接口正常！";
    }
} 