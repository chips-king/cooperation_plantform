package com.cooperation.infrastructure.security;

import com.cooperation.domain.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Spring Security 安全基础设施配置。
 */
@Configuration
public class SecurityConfig {

    private final AuthTokenService authTokenService;
    private final UserRepository userRepository;

    /**
     * 创建安全配置。
     *
     * @param authTokenService 登录令牌服务，用于校验 Bearer 令牌签名。
     * @param userRepositoryProvider 用户仓储提供器，用于生产环境校验令牌用户仍然有效。
     */
    public SecurityConfig(AuthTokenService authTokenService, ObjectProvider<UserRepository> userRepositoryProvider) {
        this.authTokenService = authTokenService;
        this.userRepository = userRepositoryProvider.getIfAvailable();
    }

    /**
     * 创建全局安全过滤链，统一接入认证入口并保护业务接口。
     *
     * @param http Spring Security HTTP 配置对象
     * @return 全局安全过滤链
     * @throws Exception 构建安全过滤链失败时抛出
     */
    @Bean
    @Order(0)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // 当前后端主要提供接口能力，先关闭 CSRF，避免非浏览器客户端认证后调用写接口被拦截。
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize
                        // 健康检查、静态资源和错误响应不需要登录，避免探活或 404 被改写成认证失败。
                        .requestMatchers(
                                "/auth/login",
                                "/auth/register",
                                "/actuator/health",
                                "/favicon.ico",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/webjars/**",
                                "/error"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/invitations/*").permitAll()
                        .anyRequest().authenticated()
                )
                // 识别登录接口签发的签名 Bearer 令牌，让前端登录后访问受保护 API。
                .addFilterBefore(new SignedBearerTokenFilter(authTokenService, userRepository), UsernamePasswordAuthenticationFilter.class)
                // 关闭 HTTP Basic，避免默认内存账号或生成密码成为额外登录入口。
                .httpBasic(AbstractHttpConfigurer::disable)
                // 未登录访问受保护接口时统一返回 401，避免默认登录页重定向干扰 API 调用方。
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .build();
    }

    /**
     * 注册空用户详情服务，阻止 Spring Boot 生成默认密码；系统登录统一走用户表和签名令牌。
     *
     * @return 不包含任何默认账号的用户详情服务。
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return new InMemoryUserDetailsManager();
    }

    /**
     * 签名 Bearer 令牌过滤器，将合法 token 转换为认证上下文。
     */
    private static final class SignedBearerTokenFilter extends OncePerRequestFilter {

        private static final String BEARER_PREFIX = "Bearer "; // HTTP Authorization 头中 Bearer 令牌的标准前缀。
        private static final String USER_ID_HEADER = "X-User-Id"; // 兼容旧版 Controller 的用户标识请求头。
        private final AuthTokenService authTokenService;
        private final UserRepository userRepository;

        private SignedBearerTokenFilter(AuthTokenService authTokenService, UserRepository userRepository) {
            this.authTokenService = authTokenService;
            this.userRepository = userRepository;
        }

        /**
         * 解析请求中的签名 Bearer 令牌并写入 Spring Security 上下文。
         *
         * @param request 当前 HTTP 请求。
         * @param response 当前 HTTP 响应。
         * @param filterChain 后续过滤器链。
         * @throws ServletException 过滤器链执行失败时抛出。
         * @throws IOException 读写请求响应失败时抛出。
         */
        @Override
        protected void doFilterInternal(
                HttpServletRequest request,
                HttpServletResponse response,
                FilterChain filterChain
        ) throws ServletException, IOException {
            String authorization = request.getHeader("Authorization");

            // 只处理登录接口签发的有效令牌，非法令牌保持匿名并交给认证入口返回 401。
            if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
                String token = authorization.substring(BEARER_PREFIX.length());
                authTokenService.authenticate(token).ifPresent(userId -> {
                    if (!isActiveUser(userId)) {
                        return;
                    }
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            String.valueOf(userId),
                            token,
                            List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                    );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                });
            }

            String authenticatedUserId = currentAuthenticatedUserId();
            if (authenticatedUserId == null) {
                filterChain.doFilter(request, response);
                return;
            }

            String headerUserId = request.getHeader(USER_ID_HEADER);
            if (headerUserId != null && !headerUserId.equals(authenticatedUserId)) {
                response.sendError(HttpStatus.FORBIDDEN.value(), "X-User-Id 与认证用户不一致");
                return;
            }

            filterChain.doFilter(new AuthenticatedUserHeaderRequest(request, authenticatedUserId), response);
        }

        private String currentAuthenticatedUserId() {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null) {
                return null;
            }
            return authentication.getName();
        }

        private boolean isActiveUser(Long userId) {
            if (userRepository == null) {
                return true;
            }
            return userRepository.findById(userId)
                    .map(user -> "active".equalsIgnoreCase(user.status()))
                    .orElse(false);
        }
    }

    /**
     * 为旧版依赖 X-User-Id 的 Controller 补充认证用户请求头，避免继续信任客户端传值。
     */
    private static final class AuthenticatedUserHeaderRequest extends HttpServletRequestWrapper {

        private static final String USER_ID_HEADER = "X-User-Id"; // 兼容旧版 Controller 的用户标识请求头。
        private final String userId;

        private AuthenticatedUserHeaderRequest(HttpServletRequest request, String userId) {
            super(request);
            this.userId = userId;
        }

        @Override
        public String getHeader(String name) {
            if (isUserIdHeader(name)) {
                return userId;
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (isUserIdHeader(name)) {
                return Collections.enumeration(List.of(userId));
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            LinkedHashSet<String> headerNames = new LinkedHashSet<>();
            Enumeration<String> originalHeaderNames = super.getHeaderNames();
            while (originalHeaderNames.hasMoreElements()) {
                headerNames.add(originalHeaderNames.nextElement());
            }
            if (headerNames.stream().noneMatch(this::isUserIdHeader)) {
                headerNames.add(USER_ID_HEADER);
            }
            return Collections.enumeration(new ArrayList<>(headerNames));
        }

        private boolean isUserIdHeader(String name) {
            return USER_ID_HEADER.toLowerCase(Locale.ROOT).equals(name == null ? "" : name.toLowerCase(Locale.ROOT));
        }
    }
}
