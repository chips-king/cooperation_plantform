package com.cooperation.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * 登录令牌服务测试，验证签名令牌和密钥强度边界。
 */
class AuthTokenServiceTest {

    /**
     * 有效签名令牌应能解析回签发用户标识。
     */
    @Test
    void shouldAuthenticateIssuedToken() {
        AuthTokenService service = new AuthTokenService("0123456789abcdef0123456789abcdef");

        String token = service.issue(42L);

        assertThat(service.authenticate(token)).contains(42L);
    }

    /**
     * 外部配置的令牌密钥过短时应拒绝启动，避免弱签名密钥进入生产。
     */
    @Test
    void shouldRejectShortConfiguredSecret() {
        assertThatThrownBy(() -> new AuthTokenService("short-secret"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("APP_TOKEN_SECRET");
    }
}
