package com.xhxi.photobooker;

import com.xhxi.photobooker.netty.NettyChatServer;
import jakarta.annotation.PostConstruct;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

@SpringBootApplication(exclude = {
    org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchClientAutoConfiguration.class,
    org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration.class,
    org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration.class,
    org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration.class
})
@EnableScheduling
public class PhotoBookerApplication {

    public static void main(String[] args) {
        System.out.println("==== 项目启动 ====");
        SpringApplication.run(PhotoBookerApplication.class, args);
    }

    @Component
    public static class NettyServerStarter implements CommandLineRunner {

        @Override
        public void run(String... args) throws Exception {
            // 延迟执行，确保Spring上下文完全初始化
            Thread.sleep(2000);
            System.out.println("==== 启动Netty WebSocket服务器 ====");
            new Thread(() -> {
                try {
                    new NettyChatServer(8081).start(); // 端口与前端一致
                } catch (Exception e) {
                    System.err.println("Netty服务器启动失败: " + e.getMessage());
                    e.printStackTrace();
                }
            }).start();
        }
    }
}
