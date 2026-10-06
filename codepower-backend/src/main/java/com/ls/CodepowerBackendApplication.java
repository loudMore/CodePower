/**
 * 文件说明：后端启动入口，负责启动 Spring Boot 应用并处理运行环境参数。
 */
package com.ls;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableConfigurationProperties
@EnableScheduling
@MapperScan("com.ls.mapper")
@ComponentScan(basePackages = {"com.ls.common", "com.ls.config", "com.ls.controller", "com.ls.service", "com.ls.utils", "com.ls.scheduler", "com.ls.websocket"})
public class CodepowerBackendApplication {

    public static void main(String[] args) {
        // 有些服务器环境变量会把 DEBUG 用作其它工具开关，这里只在显式开启本应用调试时才启用 Spring Boot debug。
        System.setProperty("debug", System.getenv().getOrDefault("SPRING_BOOT_DEBUG", "false"));
        TimeZone.setDefault(TimeZone.getTimeZone(System.getenv().getOrDefault("APP_TIME_ZONE", "Asia/Shanghai")));
        SpringApplication.run(CodepowerBackendApplication.class, args);
    }

}
