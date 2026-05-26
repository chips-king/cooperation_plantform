package com.cooperation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 分工协作系统后端应用入口。
 */
@SpringBootApplication
public class CooperationApplication {

    /**
     * 启动 Spring Boot 应用。
     *
     * @param args 命令行启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(CooperationApplication.class, args);
    }
}
