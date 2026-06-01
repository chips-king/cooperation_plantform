package com.cooperation.infrastructure.mail;

import com.cooperation.application.mail.SendMailDraftUseCase;
import com.cooperation.application.mail.SmtpConfigRepository;
import com.cooperation.domain.mail.MailDraft;
import com.cooperation.domain.mail.SmtpConfig;
import com.cooperation.infrastructure.storage.StorageProperties;
import jakarta.mail.Folder;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.Flags;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

/**
 * 基于 SMTP 的草稿发送适配器，支持从数据库动态加载 SMTP 配置。
 * 发送成功后，如果配置了 IMAP，会将邮件副本写入「已发送」文件夹。
 */
public class SmtpMailDraftSender implements SendMailDraftUseCase.MailProviderPort {

    private static final Logger log = LoggerFactory.getLogger(SmtpMailDraftSender.class);

    private static final String[] SENT_FOLDER_NAMES = {
            "Sent Messages", "已发送", "Sent", "INBOX.Sent Messages", "Sent Items"
    };

    private final MailSmtpProperties properties;
    private final StorageProperties storageProperties;
    private final JdbcTemplate jdbcTemplate;
    private final SmtpConfigRepository smtpConfigRepository;
    private final SmtpPasswordEncryptor passwordEncryptor;

    public SmtpMailDraftSender(
            MailSmtpProperties properties,
            StorageProperties storageProperties,
            JdbcTemplate jdbcTemplate,
            SmtpConfigRepository smtpConfigRepository,
            SmtpPasswordEncryptor passwordEncryptor
    ) {
        this.properties = Objects.requireNonNull(properties, "SMTP 配置不能为空");
        this.storageProperties = Objects.requireNonNull(storageProperties, "文件存储配置不能为空");
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "JDBC 模板不能为空");
        this.smtpConfigRepository = smtpConfigRepository;
        this.passwordEncryptor = passwordEncryptor;
    }

    @Override
    public void sendDraft(String draftId, MailDraft draft, String actorId, Long smtpConfigId) {
        SmtpConfig config = resolveSmtpConfig(actorId, smtpConfigId);
        String decryptedPassword;
        JavaMailSenderImpl sender;
        String fromAddress;

        if (config != null) {
            decryptedPassword = passwordEncryptor.decrypt(config.getPassword());
            config.validate();
            sender = createSenderFromConfig(config, decryptedPassword);
            fromAddress = config.getFromAddress();
        } else {
            properties.validateEnabled();
            decryptedPassword = properties.getPassword();
            sender = createSenderFromProperties();
            fromAddress = properties.getFrom();
        }

        MimeMessage message = sender.createMimeMessage();
        PackageAttachment attachment = findAttachment(draft.getPackageId());
        Path attachmentPath = resolveAttachmentPath(attachment.storageKey());
        if (!Files.isRegularFile(attachmentPath)) {
            throw new IllegalStateException("邮件附件文件不存在，请重新生成最终压缩包");
        }

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(draft.getRecipients().toArray(String[]::new));
            helper.setSubject(draft.getSubject());
            helper.setText(draft.getBody(), false);
            helper.addAttachment(attachment.filename(), attachmentPath.toFile());
        } catch (MessagingException exception) {
            throw new IllegalStateException("邮件内容组装失败", exception);
        }

        try {
            sender.send(message);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("邮件发送失败，请检查邮箱服务配置", exception);
        }

        // SMTP 发送成功后，通过 IMAP 将邮件副本写入「已发送」文件夹
        if (config != null && !config.getImapHost().isBlank()) {
            appendToSentFolder(message, config, decryptedPassword);
        }
    }

    /**
     * 通过 IMAP 将已发送邮件 APPEND 到「已发送」文件夹。
     * 此操作失败不影响发送结果，仅记录日志。
     */
    private void appendToSentFolder(MimeMessage message, SmtpConfig config, String password) {
        Store store = null;
        try {
            Properties imapProps = new Properties();
            imapProps.setProperty("mail.store.protocol", "imaps");
            imapProps.setProperty("mail.imaps.port", String.valueOf(config.getImapPort()));
            imapProps.setProperty("mail.imaps.ssl.enable", "true");
            imapProps.setProperty("mail.imaps.timeout", "10000");

            Session session = Session.getInstance(imapProps);
            store = session.getStore("imaps");
            store.connect(config.getImapHost(), config.getImapPort(),
                    config.getUsername(), password);

            Folder sentFolder = findSentFolder(store);
            if (sentFolder == null) {
                log.warn("未找到已发送文件夹，跳过 IMAP APPEND。已尝试: {}", String.join(", ", SENT_FOLDER_NAMES));
                return;
            }

            sentFolder.open(Folder.READ_WRITE);
            try {
                message.setFlag(Flags.Flag.SEEN, true);
                sentFolder.appendMessages(new MimeMessage[]{message});
                log.info("已将邮件副本写入已发送文件夹: {}", sentFolder.getFullName());
            } finally {
                if (sentFolder.isOpen()) {
                    sentFolder.close(false);
                }
            }
        } catch (Exception e) {
            log.warn("IMAP APPEND 到已发送文件夹失败（不影响邮件发送结果）: {}", e.getMessage());
        } finally {
            if (store != null) {
                try {
                    store.close();
                } catch (MessagingException ignored) {
                    // 忽略关闭异常
                }
            }
        }
    }

    /**
     * 在 IMAP 存储中查找「已发送」文件夹，依次尝试常见的文件夹名称。
     */
    private Folder findSentFolder(Store store) throws MessagingException {
        // 先尝试默认文件夹
        Folder defaultFolder = store.getDefaultFolder();

        for (String name : SENT_FOLDER_NAMES) {
            try {
                Folder folder = store.getFolder(name);
                if (folder.exists()) {
                    return folder;
                }
            } catch (MessagingException ignored) {
                // 尝试下一个名称
            }
        }

        // 遍历所有文件夹查找包含 "sent" 的
        try {
            Folder[] folders = defaultFolder.list();
            for (Folder folder : folders) {
                String fullName = folder.getFullName().toLowerCase();
                if (fullName.contains("sent") || fullName.contains("已发送")) {
                    return folder;
                }
            }
        } catch (MessagingException ignored) {
            // 无法列出文件夹
        }

        return null;
    }

    SmtpConfig resolveSmtpConfig(String actorId, Long smtpConfigId) {
        if (smtpConfigRepository == null) {
            return null;
        }
        Long createdBy = parseActorId(actorId);
        if (smtpConfigId != null) {
            SmtpConfig config = smtpConfigRepository.findById(smtpConfigId)
                    .orElseThrow(() -> new IllegalArgumentException("指定的 SMTP 配置不存在: " + smtpConfigId));
            if (!Objects.equals(config.getCreatedBy(), createdBy)) {
                throw new IllegalArgumentException("无权使用他人的 SMTP 配置");
            }
            return config;
        }
        return smtpConfigRepository.findDefaultByCreatedBy(createdBy)
                .orElseThrow(() -> new IllegalStateException("请先配置默认 SMTP"));
    }

    private Long parseActorId(String actorId) {
        try {
            return Long.valueOf(actorId);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("操作用户标识非法: " + actorId, exception);
        }
    }

    private JavaMailSenderImpl createSenderFromConfig(SmtpConfig config, String password) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(config.getHost());
        sender.setPort(config.getPort());
        sender.setUsername(config.getUsername());
        sender.setPassword(password);
        sender.setDefaultEncoding("UTF-8");
        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.ssl.enable", Boolean.toString(config.isSslEnabled()));
        props.put("mail.smtp.starttls.enable", Boolean.toString(config.isStarttlsEnabled()));
        return sender;
    }

    private JavaMailSenderImpl createSenderFromProperties() {
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
