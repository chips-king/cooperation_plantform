package com.cooperation.infrastructure.mail;

import com.cooperation.application.mail.SendMailDraftUseCase;
import com.cooperation.domain.mail.MailDraft;
import com.cooperation.infrastructure.storage.StorageProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

/**
 * 基于 SMTP 的草稿发送适配器，发送成功后才允许应用层标记草稿已发送。
 */
public class SmtpMailDraftSender implements SendMailDraftUseCase.MailProviderPort {

    private final MailSmtpProperties properties;
    private final StorageProperties storageProperties;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 创建 SMTP 草稿发送器。
     *
     * @param properties SMTP 配置
     * @param storageProperties 文件存储配置
     * @param jdbcTemplate JDBC 模板
     */
    public SmtpMailDraftSender(
            MailSmtpProperties properties,
            StorageProperties storageProperties,
            JdbcTemplate jdbcTemplate
    ) {
        this.properties = Objects.requireNonNull(properties, "SMTP 配置不能为空");
        this.storageProperties = Objects.requireNonNull(storageProperties, "文件存储配置不能为空");
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "JDBC 模板不能为空");
    }

    @Override
    public void sendDraft(String draftId, MailDraft draft) {
        properties.validateEnabled();
        PackageAttachment attachment = findAttachment(draft.getPackageId());
        Path attachmentPath = resolveAttachmentPath(attachment.storageKey());
        if (!Files.isRegularFile(attachmentPath)) {
            throw new IllegalStateException("邮件附件文件不存在，请重新生成最终压缩包");
        }

        JavaMailSenderImpl sender = createSender();
        MimeMessage message = sender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(properties.getFrom());
            helper.setTo(draft.getRecipients().toArray(String[]::new));
            helper.setSubject(draft.getSubject());
            helper.setText(draft.getBody(), false);
            helper.addAttachment(attachment.filename(), attachmentPath.toFile());
            sender.send(message);
        } catch (MessagingException exception) {
            throw new IllegalStateException("邮件内容组装失败", exception);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("邮件发送失败，请检查邮箱服务配置", exception);
        }
    }

    private JavaMailSenderImpl createSender() {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(properties.getHost());
        sender.setPort(properties.getPort());
        sender.setUsername(properties.getUsername());
        sender.setPassword(properties.getPassword());
        sender.setDefaultEncoding("UTF-8");
        sender.getJavaMailProperties().put("mail.smtp.auth", "true");
        sender.getJavaMailProperties().put("mail.smtp.ssl.enable", Boolean.toString(properties.isSslEnabled()));
        sender.getJavaMailProperties().put("mail.smtp.starttls.enable", Boolean.toString(properties.isStarttlsEnabled()));
        return sender;
    }

    private PackageAttachment findAttachment(String packageId) {
        return jdbcTemplate.queryForObject(
                "SELECT filename, storage_key FROM package_artifacts WHERE id = ?",
                (resultSet, rowNumber) -> new PackageAttachment(
                        resultSet.getString("filename"),
                        resultSet.getString("storage_key")
                ),
                packageId
        );
    }

    private Path resolveAttachmentPath(String storageKey) {
        Path root = storageProperties.getRoot().toAbsolutePath().normalize();
        Path attachment = root.resolve(storageKey).toAbsolutePath().normalize();
        if (!attachment.startsWith(root)) {
            throw new IllegalStateException("邮件附件路径非法");
        }
        return attachment;
    }

    private record PackageAttachment(String filename, String storageKey) {
    }
}
