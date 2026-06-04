package com.cooperation.integration.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import java.util.Optional;

import com.cooperation.domain.user.UserRepository;
import com.cooperation.domain.user.UserRepository.UserProfile;
import com.cooperation.infrastructure.security.PasswordEncoderConfig;
import com.cooperation.infrastructure.security.AuthTokenService;
import com.cooperation.infrastructure.security.SecurityConfig;

/**
 * 认证安全集成测试，验证匿名访问和放行入口的真实 HTTP 响应。
 */
@SpringBootTest(
        classes = SecurityIT.SecurityTestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration"
)
class SecurityIT {

    /**
     * 随机端口测试客户端，用于通过完整 Servlet 安全过滤链发起 HTTP 请求。
     */
    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AuthTokenService authTokenService;

    /**
     * 验证匿名用户访问受保护业务接口时返回 401，而不是进入控制器或跳转登录页。
     */
    @Test
    void shouldReturnUnauthorizedWhenAnonymousAccessesProtectedBusinessApi() {
        ResponseEntity<String> response = restTemplate.getForEntity("/protected", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getHeaders().getLocation()).isNull();
    }

    /**
     * 验证历史开发令牌不能伪造成有效登录态，避免任意 Bearer dev-token-* 绕过认证。
     */
    @Test
    void shouldRejectUnsignedDevelopmentBearerToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("dev-token-999");

        ResponseEntity<String> response = restTemplate.exchange(
                "/protected",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    /**
     * 验证缺少 X-User-Id 时，安全过滤链会把认证主体补充给旧版 Controller。
     */
    @Test
    void shouldPopulateUserIdHeaderFromAuthenticatedToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authTokenService.issue(7L));

        ResponseEntity<String> response = restTemplate.exchange(
                "/protected/user-id",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("7");
    }

    /**
     * 验证客户端不能通过 X-User-Id 冒用其他用户身份。
     */
    @Test
    void shouldRejectMismatchedUserIdHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authTokenService.issue(7L));
        headers.set("X-User-Id", "8");

        ResponseEntity<String> response = restTemplate.exchange(
                "/protected/user-id",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    /**
     * 验证已停用用户的签名令牌不能继续访问受保护接口。
     */
    @Test
    void shouldRejectInactiveUserToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authTokenService.issue(9L));

        ResponseEntity<String> response = restTemplate.exchange(
                "/protected",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    /**
     * 验证静态资源与显式放行入口不会被认证入口改写成登录页重定向。
     */
    @Test
    void shouldNotRedirectPermitAllAndStaticEntriesToLoginPage() {
        assertNotRedirectedToLoginPage(restTemplate.getForEntity("/actuator/health", String.class));
        assertNotRedirectedToLoginPage(restTemplate.getForEntity("/favicon.ico", String.class));
    }

    /**
     * 断言放行入口不返回认证失败或登录页跳转，实际资源不存在时允许由 MVC 返回 404。
     *
     * @param response 放行入口的 HTTP 响应
     */
    private void assertNotRedirectedToLoginPage(ResponseEntity<String> response) {
        assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getStatusCode().is3xxRedirection()).isFalse();
        assertThat(response.getHeaders().getLocation())
                .as("放行入口不应携带登录页跳转地址")
                .satisfies(location -> {
                    // 只有响应真的包含跳转地址时，才继续检查目标是否为登录页。
                    if (location != null) {
                        assertThat(location.toString()).doesNotContain("/login");
                    }
                });
    }

    /**
     * 安全集成测试专用最小应用，只加载安全配置和一个受保护端点，避免业务 Controller 依赖影响安全链路验证。
     */
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({SecurityConfig.class, PasswordEncoderConfig.class, AuthTokenService.class, ProtectedController.class})
    static class SecurityTestApplication {

        /**
         * 提供测试用户仓储，让安全过滤链能验证令牌用户是否仍为 active 状态。
         *
         * @return 测试用户仓储。
         */
        @Bean
        UserRepository userRepository() {
            return new UserRepository() {
                @Override
                public Optional<UserProfile> findById(Long id) {
                    if (id == 9L) {
                        return Optional.of(new UserProfile(9L, "停用用户", "disabled@example.com", "disabled"));
                    }
                    return Optional.of(new UserProfile(id, "正常用户", "active@example.com", "active"));
                }

                @Override
                public Optional<UserProfile> findByEmail(String email) {
                    return Optional.empty();
                }

                @Override
                public Optional<UserProfile> findByLoginAccount(String account) {
                    return Optional.empty();
                }

                @Override
                public boolean updateProfile(Long id, String displayName, String email) {
                    return false;
                }

                @Override
                public Optional<String> findPasswordHashById(Long id) {
                    return Optional.empty();
                }

                @Override
                public boolean updatePassword(Long id, String passwordHash) {
                    return false;
                }

                @Override
                public Optional<UserProfile> findByUsername(String username) {
                    return Optional.empty();
                }

                @Override
                public Optional<UserProfile> createUser(String username, String displayName, String email, String passwordHash) {
                    return Optional.empty();
                }
            };
        }
    }

    /**
     * 测试专用受保护端点，匿名访问应在进入控制器前被安全过滤链拦截。
     */
    @RestController
    static class ProtectedController {

        /**
         * 返回受保护资源内容。
         *
         * @return 固定测试内容
         */
        @GetMapping("/protected")
        String protectedResource() {
            return "ok";
        }

        /**
         * 返回控制器实际收到的用户标识请求头，用于验证安全过滤链的兼容桥。
         *
         * @param userId 当前用户标识请求头。
         * @return 当前用户标识文本。
         */
        @GetMapping("/protected/user-id")
        String protectedUserId(@RequestHeader("X-User-Id") String userId) {
            return userId;
        }
    }
}
