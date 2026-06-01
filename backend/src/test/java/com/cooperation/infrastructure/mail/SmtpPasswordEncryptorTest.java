package com.cooperation.infrastructure.mail;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SMTP 密码加解密器测试，约束密钥格式和加解密往返行为。
 */
class SmtpPasswordEncryptorTest {

    private static final String AES_KEY =
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    @Test
    void encryptAndDecryptReturnsOriginalPlaintext() {
        SmtpPasswordEncryptor encryptor = new SmtpPasswordEncryptor(AES_KEY);

        String ciphertext = encryptor.encrypt("smtp-password");

        assertThat(ciphertext).isNotEqualTo("smtp-password");
        assertThat(encryptor.decrypt(ciphertext)).isEqualTo("smtp-password");
    }

    @Test
    void rejectBlankAesKey() {
        assertThatThrownBy(() -> new SmtpPasswordEncryptor(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不能为空");
    }

    @Test
    void rejectNonHexAesKey() {
        assertThatThrownBy(() -> new SmtpPasswordEncryptor(
                "z123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("64 位十六进制");
    }

    @Test
    void rejectWrongLengthAesKey() {
        assertThatThrownBy(() -> new SmtpPasswordEncryptor("0123456789abcdef"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("64 位十六进制");
    }
}
