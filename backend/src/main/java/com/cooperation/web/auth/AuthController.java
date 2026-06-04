package com.cooperation.web.auth;

import com.cooperation.domain.user.UserRepository;
import com.cooperation.domain.user.UserRepository.UserProfile;
import com.cooperation.infrastructure.security.AuthTokenService;
import com.cooperation.web.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录 Web API 控制器，提供前端会话建立所需的账号密码登录接口。
 */
@RestController
public class AuthController {

    private static final List<String> DEFAULT_PERMISSIONS = List.of(
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

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthTokenService authTokenService;

    /**
     * 创建登录控制器实例。
     *
     * @param userRepository 用户仓储，用于查询用户信息。
     * @param passwordEncoder 密码编码器，用于验证密码。
     * @param authTokenService 登录令牌服务，用于签发带签名的 Bearer 令牌。
     */
    public AuthController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthTokenService authTokenService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authTokenService = authTokenService;
    }

    /**
     * 使用账号密码登录系统。
     *
     * @param request 登录请求，包含账号和密码。
     * @return 登录成功时返回会话信息，失败时返回未授权错误。
     */
    @PostMapping("/auth/login")
    public ResponseEntity<ApiResponse<AuthDto.LoginResponse>> login(
            @Valid @RequestBody AuthDto.LoginRequest request) {

        // 1. 按账号（展示名称或邮箱）查找用户
        Optional<UserProfile> userOpt = userRepository.findByLoginAccount(request.account());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.failure("AUTH_FAILED", "账号或密码错误", null));
        }

        UserProfile user = userOpt.get();

        // 2. 查询该用户的密码哈希并验证
        Optional<String> passwordHashOpt = userRepository.findPasswordHashById(user.id());
        if (passwordHashOpt.isEmpty() || passwordHashOpt.get() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.failure("AUTH_FAILED", "该账户尚未设置密码", null));
        }

        if (!passwordEncoder.matches(request.password(), passwordHashOpt.get())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.failure("AUTH_FAILED", "账号或密码错误", null));
        }

        // 3. 签发带签名的登录令牌并返回用户信息。
        String token = authTokenService.issue(user.id());
        AuthDto.CurrentUserResponse userResponse = new AuthDto.CurrentUserResponse(
                user.id(),
                user.displayName(),
                user.email(),
                user.status()
        );
        AuthDto.LoginResponse response = new AuthDto.LoginResponse(
                userResponse, token, DEFAULT_PERMISSIONS);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 注册新用户。
     *
     * @param request 注册请求，包含用户名、密码、展示名称和邮箱。
     * @return 注册成功时返回登录响应，失败时返回冲突错误。
     */
    @PostMapping("/auth/register")
    public ResponseEntity<ApiResponse<AuthDto.LoginResponse>> register(
            @Valid @RequestBody AuthDto.RegisterRequest request) {

        // 1. 检查用户名是否已被占用
        if (userRepository.findByUsername(request.username()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.failure("USERNAME_EXISTS", "该用户名已被注册", null));
        }

        // 2. 检查邮箱是否已被占用
        if (userRepository.findByEmail(request.email()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.failure("EMAIL_EXISTS", "该邮箱已被注册", null));
        }

        // 3. 加密密码并创建用户
        String passwordHash = passwordEncoder.encode(request.password());
        Optional<UserProfile> newUserOpt = userRepository.createUser(
                request.username(),
                request.displayName(),
                request.email(),
                passwordHash
        );

        if (newUserOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.failure("REGISTER_FAILED", "注册失败，请稍后重试", null));
        }

        // 4. 注册成功后自动签发带签名令牌，方便前端直接跳转。
        UserProfile newUser = newUserOpt.get();
        String token = authTokenService.issue(newUser.id());
        AuthDto.CurrentUserResponse userResponse = new AuthDto.CurrentUserResponse(
                newUser.id(),
                newUser.displayName(),
                newUser.email(),
                newUser.status()
        );
        AuthDto.LoginResponse response = new AuthDto.LoginResponse(
                userResponse, token, DEFAULT_PERMISSIONS);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }
}
