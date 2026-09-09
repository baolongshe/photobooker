package com.xhxi.photobooker.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AlipayConfig {
    // 从配置文件读取沙箱参数
    @Value("${alipay.app-id}")
    private String appId;
    @Value("${alipay.private-key}")
    private String privateKey; // 商户私钥
    @Value("${alipay.public-key}")
    private String publicKey;  // 支付宝公钥
    @Value("${alipay.gateway-url}")
    private String gatewayUrl; // 沙箱网关

    @Bean
    public AlipayClient alipayClient() {
        // 初始化支付宝客户端
        return new DefaultAlipayClient(
                gatewayUrl,    // 沙箱网关
                appId,         // 沙箱APPID
                privateKey,    // 商户私钥
                "json",        // 格式
                "UTF-8",       // 编码
                publicKey,     // 支付宝公钥
                "RSA2"         // 签名算法
        );
    }
}