package com.cooperation.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
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

    private static final String DEFAULT_USERNAME = "admin"; // 临时内存账号用户名，用于真实用户表接入前的最小认证。
    private static final String DEFAULT_PASSWORD = "123456"; // 临时内存账号密码，仅用于当前阶段本地安全链路验证。
    private static final String DEFAULT_ROLE = "ADMIN"; // 临时内存账号角色，后续接入权限模型后替换为真实角色来源。

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
                                "/actuator/health",
                                "/favicon.ico",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/webjars/**",
                                "/error"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                // 识别开发期登录接口返回的 Bearer 令牌，让前端登录后可直接访问受保护 API。
                .addFilterBefore(new DevBearerTokenFilter(), UsernamePasswordAuthenticationFilter.class)
                // 使用 HTTP Basic 建立最小账号密码登录链路，后续可替换为真实用户表或表单登录。
                .httpBasic(Customizer.withDefaults())
                // 未登录访问受保护接口时统一返回 401，避免默认登录页重定向干扰 API 调用方。
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .build();
    }

    /**
     * 创建测试期内存用户服务，作为完整用户表接入前的最小认证来源。
     *
     * @param passwordEncoder 密码编码器
     * @return 内存用户详情服务
     */
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails defaultUser = User.withUsername(DEFAULT_USERNAME)
                .password(passwordEncoder.encode(DEFAULT_PASSWORD))
                .roles(DEFAULT_ROLE)
                .build();
        return new InMemoryUserDetailsManager(defaultUser);
    }

    /**
     * 开发期 Bearer 令牌过滤器，将 dev-token-用户标识 转换为认证上下文。
     */
    private static final class DevBearerTokenFilter extends OncePerRequestFilter {

        private static final String BEARER_PREFIX = "Bearer "; // HTTP Authorization 头中 Bearer 令牌的标准前缀。
        private static final String DEV_TOKEN_PREFIX = "dev-token-"; // 本地开发登录令牌前缀，后续替换为真实 JWT 校验。

        /**
         * 解析请求中的开发期 Bearer 令牌并写入 Spring Security 上下文。
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

            // 只处理本地开发登录接口签发的令牌，其他认证方式继续交给后续过滤器。
            if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
                String token = authorization.substring(BEARER_PREFIX.length());
                if (token.startsWith(DEV_TOKEN_PREFIX)) {
                    String userId = token.substring(DEV_TOKEN_PREFIX.length());
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userId,
                            token,
                            List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                    );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }

            filterChain.doFilter(request, response);
        }
    }
}
