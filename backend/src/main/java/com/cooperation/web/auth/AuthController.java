package com.cooperation.web.auth;

import com.cooperation.web.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录 Web API 控制器，提供前端会话建立所需的最小账号密码登录接口。
 */
@RestController
public class AuthController {

    private static final long DEFAULT_ADMIN_ID = 1L; // 默认管理员用户标识，用于真实用户认证接入前的开发登录。
    private static final String DEFAULT_ADMIN_USERNAME = "admin"; // 默认管理员账号名。
    private static final String DEFAULT_ADMIN_EMAIL = "admin@example.com"; // 默认管理员邮箱账号。
    private static final String DEFAULT_ADMIN_PASSWORD = "123456"; // 默认管理员密码，仅用于本地开发验证。
    private static final String DEFAULT_ADMIN_TOKEN = "dev-token-1"; // 默认管理员访问令牌，后续替换为真实令牌服务。

    private static final List<String> DEFAULT_ADMIN_PERMISSIONS = List.of(
            "project.view",
            "project.manage",
            "member.manage",
            "permission.manage",
            "file.view",
            "file.upload",
            "file.download",
            "file.move",
            "file.rename",
            "file.delete",
            "file.restore",
            "directory.manage",
            "directory.status.update",
            "check.run",
            "cleanup.apply",
            "package.create",
            "package.download",
            "mail.draft.create",
            "mail.draft.update",
            "mail.send",
            "log.view",
            "notification.view",
            "project.end",
            "project.reopen"
    );

    /**
     * 使用账号密码登录系统。
     *
     * @param request 登录请求。
     * @return 登录成功时返回会话信息，失败时返回未授权错误。
     */
    @PostMapping("/auth/login")
    public ResponseEntity<ApiResponse<AuthDto.LoginResponse>> login(@Valid @RequestBody AuthDto.LoginRequest request) {
        if (!isDefaultAdminCredential(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.failure("AUTH_FAILED", "账号或密码错误", null));
        }

        AuthDto.CurrentUserResponse user = new AuthDto.CurrentUserResponse(
                DEFAULT_ADMIN_ID,
                "管理员",
                DEFAULT_ADMIN_EMAIL,
                "active"
        );
        AuthDto.LoginResponse response = new AuthDto.LoginResponse(
                user,
                DEFAULT_ADMIN_TOKEN,
                DEFAULT_ADMIN_PERMISSIONS
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 判断请求是否匹配默认管理员凭据。
     *
     * @param request 登录请求。
     * @return 凭据正确时返回 true。
     */
    private boolean isDefaultAdminCredential(AuthDto.LoginRequest request) {
        boolean accountMatched = DEFAULT_ADMIN_USERNAME.equals(request.account())
                || DEFAULT_ADMIN_EMAIL.equalsIgnoreCase(request.account());
        return accountMatched && DEFAULT_ADMIN_PASSWORD.equals(request.password());
    }
}
