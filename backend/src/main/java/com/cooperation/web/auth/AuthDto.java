package com.cooperation.web.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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

    /**
     * 用户注册请求。
     *
     * @param username 登录用户名。
     * @param password 登录密码。
     * @param displayName 展示名称。
     * @param email 邮箱。
     */
    public record RegisterRequest(
            @NotBlank(message = "用户名不能为空")
            @Size(min = 2, max = 50, message = "用户名长度需在 2 到 50 个字符之间")
            String username,

            @NotBlank(message = "密码不能为空")
            @Size(min = 6, max = 100, message = "密码长度需在 6 到 100 个字符之间")
            String password,

            @NotBlank(message = "展示名称不能为空")
            @Size(max = 100, message = "展示名称不能超过 100 个字符")
            String displayName,

            @NotBlank(message = "邮箱不能为空")
            @Pattern(regexp = "^[^\\s@]+@[^\\s@]+\\.(com|cn|net|org|edu|gov|io|cc|vip|info|top|club|xyz|me|co\\.cn|com\\.cn|net\\.cn|org\\.cn)$", message = "邮箱格式不正确")
            @Size(max = 255, message = "邮箱不能超过 255 个字符")
            String email
    ) {
    }
}
