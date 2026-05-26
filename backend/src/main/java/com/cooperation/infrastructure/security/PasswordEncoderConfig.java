package com.cooperation.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码加密基础设施配置，统一注册 Spring Security 的密码编码器 Bean。
 */
@Configuration
public class PasswordEncoderConfig {

    /**
     * 创建 BCrypt 密码编码器，用于保存密码前生成不可逆哈希，避免明文密码落库。
     *
     * @return 基于 BCrypt 算法的密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
