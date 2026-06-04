package com.cooperation.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 登录令牌服务，负责签发和校验带 HMAC 签名的短期 Bearer 令牌。
 */
@Component
public class AuthTokenService {

    private static final String TOKEN_VERSION = "v1"; // 当前令牌格式版本，用于后续平滑升级解析逻辑。
    private static final String HMAC_ALGORITHM = "HmacSHA256"; // 令牌签名算法，避免无签名令牌被伪造。
    private static final int MIN_SECRET_BYTES = 32; // HMAC 密钥最小字节数，降低生产弱密钥误配置风险。
    private static final Duration TOKEN_TTL = Duration.ofHours(12); // 登录令牌有效期，兼顾课堂协作时长和泄露风险。
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final byte[] secret;

    /**
     * 创建登录令牌服务。
     *
     * @param configuredSecret 环境变量提供的令牌签名密钥，留空时生成仅当前进程有效的随机密钥。
     */
    public AuthTokenService(@Value("${app.security.token-secret:}") String configuredSecret) {
        this.secret = resolveSecret(configuredSecret);
    }

    /**
     * 为指定用户签发 Bearer 令牌。
     *
     * @param userId 用户唯一标识。
     * @return 可放入 Authorization Bearer 头的令牌。
     */
    public String issue(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("用户标识不能为空");
        }
        long expiresAt = Instant.now().plus(TOKEN_TTL).getEpochSecond();
        String payload = TOKEN_VERSION + "." + userId + "." + expiresAt;
        return payload + "." + sign(payload);
    }

    /**
     * 校验 Bearer 令牌并解析用户标识。
     *
     * @param token 请求头中的 Bearer 令牌。
     * @return 令牌合法且未过期时返回用户标识，否则返回空。
     */
    public Optional<Long> authenticate(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        String[] parts = token.split("\\.");
        if (parts.length != 4 || !TOKEN_VERSION.equals(parts[0])) {
            return Optional.empty();
        }

        String payload = parts[0] + "." + parts[1] + "." + parts[2];
        if (!constantTimeEquals(sign(payload), parts[3])) {
            return Optional.empty();
        }

        try {
            long userId = Long.parseLong(parts[1]);
            long expiresAt = Long.parseLong(parts[2]);
            if (userId <= 0 || expiresAt < Instant.now().getEpochSecond()) {
                return Optional.empty();
            }
            return Optional.of(userId);
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private byte[] resolveSecret(String configuredSecret) {
        if (configuredSecret != null && !configuredSecret.isBlank()) {
            byte[] configuredSecretBytes = configuredSecret.trim().getBytes(StandardCharsets.UTF_8);
            if (configuredSecretBytes.length < MIN_SECRET_BYTES) {
                throw new IllegalArgumentException("APP_TOKEN_SECRET 至少需要 32 字节随机字符串");
            }
            return configuredSecretBytes;
        }
        byte[] generated = new byte[32];
        SECURE_RANDOM.nextBytes(generated);
        return generated;
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("签发登录令牌失败", exception);
        }
    }

    private boolean constantTimeEquals(String expected, String actual) {
        byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = actual == null ? new byte[0] : actual.getBytes(StandardCharsets.UTF_8);
        return java.security.MessageDigest.isEqual(expectedBytes, actualBytes);
    }
}
