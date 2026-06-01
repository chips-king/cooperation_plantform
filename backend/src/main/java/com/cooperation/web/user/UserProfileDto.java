package com.cooperation.web.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 用户资料 Web API 数据传输对象集合。
 */
public final class UserProfileDto {

    private UserProfileDto() {
    }

    /**
     * 用户资料响应。
     *
     * @param id 用户唯一标识。
     * @param displayName 用户展示名称。
     * @param email 用户邮箱。
     * @param status 用户状态。
     */
    public record ProfileResponse(Long id, String displayName, String email, String status) {
    }

    /**
     * 更新用户资料请求。
     *
     * @param displayName 新的展示名称。
     * @param email 新的邮箱。
     */
    public record UpdateProfileRequest(
            @NotBlank(message = "展示名称不能为空")
            @Size(max = 100, message = "展示名称不能超过 100 个字符")
            String displayName,

            @NotBlank(message = "邮箱不能为空")
            @Pattern(regexp = "^[^\\s@]+@[^\\s@]+\\.(com|cn|net|org|edu|gov|io|cc|vip|info|top|club|xyz|me|co\\.cn|com\\.cn|net\\.cn|org\\.cn)$", message = "邮箱格式不正确")
            @Size(max = 255, message = "邮箱不能超过 255 个字符")
            String email
    ) {
    }

    /**
     * 修改密码请求。
     *
     * @param currentPassword 当前密码。
     * @param newPassword 新密码。
     */
    public record ChangePasswordRequest(
            @NotBlank(message = "请输入当前密码")
            String currentPassword,

            @NotBlank(message = "请输入新密码")
            @Size(min = 6, max = 100, message = "密码长度需在 6 到 100 个字符之间")
            String newPassword
    ) {
    }
}
