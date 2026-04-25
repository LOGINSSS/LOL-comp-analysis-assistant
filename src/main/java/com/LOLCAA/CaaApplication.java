package com.LOLCAA;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring Boot 应用启动入口。
 *
 * - @EnableScheduling: 启用定时任务（对局采集调度）
 * - @EnableAsync: 启用异步能力（后续可用于并行分析任务）
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class CaaApplication {
    public static void main(String[] args) {
        SpringApplication.run(CaaApplication.class, args);
    }
}