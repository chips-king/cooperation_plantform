package com.cooperation.web.user;

import com.cooperation.domain.user.UserRepository;
import com.cooperation.domain.user.UserRepository.UserProfile;
import com.cooperation.web.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户资料 Web API 控制器，提供当前登录用户的资料查询和编辑接口。
 */
@RestController
public class UserProfileController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 创建用户资料控制器实例。
     *
     * @param userRepository 用户仓储，用于查询和更新用户资料。
     * @param passwordEncoder 密码编码器，用于密码校验和加密。
     */
    public UserProfileController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 获取当前登录用户的资料。
     *
     * @param headerUserId 请求头中的当前用户标识（由前端 HTTP 层自动注入 X-User-Id）。
     * @return 用户资料响应，用户不存在时返回 404。
     */
    @GetMapping("/users/me")
    public ResponseEntity<ApiResponse<UserProfileDto.ProfileResponse>> getCurrentUser(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId
    ) {
        if (headerUserId == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("MISSING_USER_ID", "缺少用户标识", null));
        }

        return userRepository.findById(headerUserId)
                .map(this::toProfileResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.failure("USER_NOT_FOUND", "用户不存在", null)));
    }

    /**
     * 更新当前登录用户的资料（展示名称和邮箱）。
     *
     * @param headerUserId 请求头中的当前用户标识。
     * @param request 更新请求，包含新的展示名称和邮箱。
     * @return 更新后的用户资料响应；邮箱已被其他用户占用时返回 409。
     */
    @PutMapping("/users/me")
    public ResponseEntity<ApiResponse<UserProfileDto.ProfileResponse>> updateCurrentUser(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @Valid @RequestBody UserProfileDto.UpdateProfileRequest request
    ) {
        if (headerUserId == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("MISSING_USER_ID", "缺少用户标识", null));
        }

        // 检查邮箱是否被其他用户占用
        Optional<UserProfile> existingUser = userRepository.findByEmail(request.email());
        if (existingUser.isPresent() && !existingUser.get().id().equals(headerUserId)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.failure("EMAIL_ALREADY_USED", "该邮箱已被其他用户使用", null));
        }

        boolean updated = userRepository.updateProfile(headerUserId, request.displayName(), request.email());
        if (!updated) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.failure("USER_NOT_FOUND", "用户不存在", null));
        }

        return userRepository.findById(headerUserId)
                .map(this::toProfileResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.failure("USER_NOT_FOUND", "更新后查询用户失败", null)));
    }

    /**
     * 修改当前登录用户的密码。
     *
     * @param headerUserId 请求头中的当前用户标识。
     * @param request 修改密码请求，包含当前密码和新密码。
     * @return 成功时返回成功响应；当前密码错误时返回 400。
     */
    @PutMapping("/users/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @Valid @RequestBody UserProfileDto.ChangePasswordRequest request
    ) {
        if (headerUserId == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("MISSING_USER_ID", "缺少用户标识", null));
        }

        // 查询当前密码哈希
        Optional<String> currentHash = userRepository.findPasswordHashById(headerUserId);
        if (currentHash.isEmpty() || currentHash.get() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.failure("PASSWORD_NOT_SET", "当前账户尚未设置密码", null));
        }

        // 验证当前密码
        if (!passwordEncoder.matches(request.currentPassword(), currentHash.get())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("INVALID_CURRENT_PASSWORD", "当前密码不正确", null));
        }

        // 加密新密码并更新
        String newHash = passwordEncoder.encode(request.newPassword());
        boolean updated = userRepository.updatePassword(headerUserId, newHash);
        if (!updated) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.failure("USER_NOT_FOUND", "用户不存在", null));
        }

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 将领域层用户摘要转换为 Web 层响应 DTO。
     *
     * @param profile 领域层用户摘要。
     * @return 包装在统一响应结构中的用户资料。
     */
    private ApiResponse<UserProfileDto.ProfileResponse> toProfileResponse(UserProfile profile) {
        return ApiResponse.success(new UserProfileDto.ProfileResponse(
                profile.id(),
                profile.displayName(),
                profile.email(),
                profile.status()
        ));
    }
}
