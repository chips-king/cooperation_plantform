package com.cooperation.infrastructure.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 当前用户上下文提供器，负责从 Spring Security 上下文读取当前用户标识。
 */
@Component
public class CurrentUserProvider {

    /**
     * 获取当前已认证用户的用户标识。
     *
     * @return 当前用户标识。
     * @throws IllegalStateException 当前请求没有认证信息或认证名称不是合法 Long 时抛出。
     */
    public Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("当前用户未认证，无法获取用户标识");
        }

        String principalName = authentication.getName();
        if (principalName == null || principalName.isBlank()) {
            throw new IllegalStateException("当前用户认证名称为空，无法获取用户标识");
        }

        try {
            return Long.parseLong(principalName);
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("当前用户认证名称不是合法的 Long 用户标识: " + principalName, exception);
        }
    }
}
