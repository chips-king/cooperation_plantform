package com.cooperation.infrastructure.mail;

import com.cooperation.application.mail.SmtpConfigRepository;
import com.cooperation.domain.mail.SmtpConfig;
import com.cooperation.infrastructure.storage.StorageProperties;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * SMTP 草稿发送适配器测试，约束发送时只能使用操作者自己的 SMTP 配置。
 */
class SmtpMailDraftSenderTest {

    private static final String TEST_AES_KEY =
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    @Test
    void resolveSmtpConfigRejectsConfigOwnedByAnotherUser() {
        FakeSmtpConfigRepository smtpConfigRepository = new FakeSmtpConfigRepository();
        smtpConfigRepository.config = config(7L, 2002L, false);
        SmtpMailDraftSender sender = sender(smtpConfigRepository);

        assertThatThrownBy(() -> sender.resolveSmtpConfig("1001", 7L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("无权使用");
    }

    @Test
    void resolveSmtpConfigUsesActorDefaultConfigWhenExplicitConfigMissing() {
        FakeSmtpConfigRepository smtpConfigRepository = new FakeSmtpConfigRepository();
        smtpConfigRepository.defaultConfig = config(8L, 1001L, true);
        SmtpMailDraftSender sender = sender(smtpConfigRepository);

        SmtpConfig resolved = sender.resolveSmtpConfig("1001", null);

        assertThat(resolved.getId()).isEqualTo(8L);
    }

    private SmtpMailDraftSender sender(SmtpConfigRepository smtpConfigRepository) {
        return new SmtpMailDraftSender(
                new MailSmtpProperties(),
                new StorageProperties(),
                mock(JdbcTemplate.class),
                smtpConfigRepository,
                new SmtpPasswordEncryptor(TEST_AES_KEY)
        );
    }

    private SmtpConfig config(Long id, Long createdBy, boolean isDefault) {
        return SmtpConfig.create(
                "测试 SMTP", "smtp.example.com", 465, "user", "encrypted",
                "from@example.com", "", 993, true, false, isDefault, createdBy
        ).withId(id);
    }

    /**
     * SMTP 配置仓储假实现，支持按标识和默认配置查询。
     */
    private static final class FakeSmtpConfigRepository implements SmtpConfigRepository {

        private SmtpConfig config;
        private SmtpConfig defaultConfig;

        @Override
        public SmtpConfig save(SmtpConfig config) {
            this.config = config;
            return config;
        }

        @Override
        public Optional<SmtpConfig> findById(Long id) {
            return Optional.ofNullable(config).filter(saved -> saved.getId().equals(id));
        }

        @Override
        public List<SmtpConfig> findByCreatedBy(Long createdBy) {
            return List.of();
        }

        @Override
        public Optional<SmtpConfig> findDefaultByCreatedBy(Long createdBy) {
            return Optional.ofNullable(defaultConfig)
                    .filter(saved -> saved.getCreatedBy().equals(createdBy));
        }

        @Override
        public void clearDefault(Long createdBy) {
        }

        @Override
        public void deleteById(Long id) {
        }
    }
}
