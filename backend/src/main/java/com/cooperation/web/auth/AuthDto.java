package com.cooperation.web.auth;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * 登录 Web API 数据传输对象集合。
 */
public final class AuthDto {

    private AuthDto() {
    }

    /**
     * 账号密码登录请求。
     *
     * @param account 登录账号，可使用管理员用户名或邮箱。
     * @param password 登录密码。
     */
    public record LoginRequest(
            @NotBlank(message = "账号不能为空") String account,
            @NotBlank(message = "密码不能为空") String password
    ) {
    }

    /**
     * 当前登录用户摘要。
     *
     * @param id 用户标识。
     * @param displayName 用户展示名称。
     * @param email 用户邮箱。
     * @param status 用户状态。
     */
    public record CurrentUserResponse(Long id, String displayName, String email, String status) {
    }

    /**
     * 登录成功响应。
     *
     * @param user 当前用户摘要。
     * @param token 访问令牌。
     * @param permissions 当前用户权限摘要。
     */
    public record LoginResponse(CurrentUserResponse user, String token, List<String> permissions) {
    }
}
