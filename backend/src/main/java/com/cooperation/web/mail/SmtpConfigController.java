package com.cooperation.web.mail;

import com.cooperation.application.mail.SmtpConfigRepository;
import com.cooperation.domain.mail.SmtpConfig;
import com.cooperation.infrastructure.mail.SmtpPasswordEncryptor;
import com.cooperation.web.common.ApiResponse;
import jakarta.mail.internet.MimeMessage;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Properties;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * SMTP 邮件配置管理控制器，提供配置的增删改查和测试发送功能。
 */
@RestController
public class SmtpConfigController {

    private final SmtpConfigRepository smtpConfigRepository;
    private final SmtpPasswordEncryptor passwordEncryptor;

    public SmtpConfigController(SmtpConfigRepository smtpConfigRepository,
                                SmtpPasswordEncryptor passwordEncryptor) {
        this.smtpConfigRepository = smtpConfigRepository;
        this.passwordEncryptor = passwordEncryptor;
    }

    /**
     * 查询当前用户的所有 SMTP 配置。
     */
    @GetMapping("/smtp-configs")
    public ApiResponse<List<SmtpConfigDto.SmtpConfigResponse>> listConfigs(
            @RequestHeader("X-User-Id") Long userId
    ) {
        List<SmtpConfig> configs = smtpConfigRepository.findByCreatedBy(userId);
        List<SmtpConfigDto.SmtpConfigResponse> responses = configs.stream()
                .map(this::toResponse)
                .toList();
        return ApiResponse.success(responses);
    }

    /**
     * 新增 SMTP 配置。
     */
    @PostMapping("/smtp-configs")
    public ApiResponse<SmtpConfigDto.SmtpConfigResponse> createConfig(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody SmtpConfigDto.SaveSmtpConfigRequest request
    ) {
        if (request.password() == null || request.password().isBlank()) {
            throw new IllegalArgumentException("登录密码不能为空");
        }
        boolean isDefault = request.isDefault() != null && request.isDefault();
        if (isDefault) {
            smtpConfigRepository.clearDefault(userId);
        }

        String encryptedPassword = passwordEncryptor.encrypt(request.password());
        SmtpConfig config = SmtpConfig.create(
                request.name(), request.host(), request.port(),
                request.username(), encryptedPassword, request.fromAddress(),
                request.imapHost() != null ? request.imapHost() : "",
                request.imapPort() != null ? request.imapPort() : 993,
                request.sslEnabled() != null ? request.sslEnabled() : true,
                request.starttlsEnabled() != null ? request.starttlsEnabled() : false,
                isDefault, userId
        );

        SmtpConfig saved = smtpConfigRepository.save(config);
        return ApiResponse.success(toResponse(saved));
    }

    /**
     * 修改 SMTP 配置。
     */
    @PutMapping("/smtp-configs/{id}")
    public ApiResponse<SmtpConfigDto.SmtpConfigResponse> updateConfig(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody SmtpConfigDto.SaveSmtpConfigRequest request
    ) {
        SmtpConfig existing = smtpConfigRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("SMTP 配置不存在: " + id));
        if (!existing.getCreatedBy().equals(userId)) {
            throw new IllegalArgumentException("无权修改他人的 SMTP 配置");
        }

        boolean isDefault = request.isDefault() != null && request.isDefault();
        if (isDefault) {
            smtpConfigRepository.clearDefault(userId);
        }

        String encryptedPassword = request.password() == null || request.password().isBlank()
                ? existing.getPassword()
                : passwordEncryptor.encrypt(request.password());
        SmtpConfig updated = SmtpConfig.create(
                request.name(), request.host(), request.port(),
                request.username(), encryptedPassword, request.fromAddress(),
                request.imapHost() != null ? request.imapHost() : "",
                request.imapPort() != null ? request.imapPort() : 993,
                request.sslEnabled() != null ? request.sslEnabled() : true,
                request.starttlsEnabled() != null ? request.starttlsEnabled() : false,
                isDefault, userId
        ).withId(id);

        SmtpConfig saved = smtpConfigRepository.save(updated);
        return ApiResponse.success(toResponse(saved));
    }

    /**
     * 删除 SMTP 配置。
     */
    @DeleteMapping("/smtp-configs/{id}")
    public ApiResponse<Void> deleteConfig(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId
    ) {
        SmtpConfig existing = smtpConfigRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("SMTP 配置不存在: " + id));
        if (!existing.getCreatedBy().equals(userId)) {
            throw new IllegalArgumentException("无权删除他人的 SMTP 配置");
        }
        smtpConfigRepository.deleteById(id);
        return ApiResponse.successWithoutData();
    }

    /**
     * 发送测试邮件验证 SMTP 配置是否可用。
     */
    @PostMapping("/smtp-configs/{id}/test")
    public ApiResponse<SmtpConfigDto.TestSmtpResponse> testConfig(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody SmtpConfigDto.TestSmtpRequest request
    ) {
        SmtpConfig config = smtpConfigRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("SMTP 配置不存在: " + id));
        if (!config.getCreatedBy().equals(userId)) {
            throw new IllegalArgumentException("无权使用他人的 SMTP 配置");
        }

        try {
            String decryptedPassword = passwordEncryptor.decrypt(config.getPassword());
            JavaMailSenderImpl sender = createSender(config, decryptedPassword);
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(config.getFromAddress());
            helper.setTo(request.testRecipient());
            helper.setSubject("SMTP 配置测试");
            helper.setText("这是一封测试邮件，如果你收到此邮件说明 SMTP 配置正确。", false);
            sender.send(message);
            return ApiResponse.success(new SmtpConfigDto.TestSmtpResponse(true, "测试邮件发送成功"));
        } catch (Exception e) {
            return ApiResponse.success(new SmtpConfigDto.TestSmtpResponse(false, "发送失败: " + e.getMessage()));
        }
    }

    /**
     * 设为默认 SMTP 配置。
     */
    @PutMapping("/smtp-configs/{id}/default")
    public ApiResponse<Void> setDefault(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId
    ) {
        SmtpConfig config = smtpConfigRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("SMTP 配置不存在: " + id));
        if (!config.getCreatedBy().equals(userId)) {
            throw new IllegalArgumentException("无权修改他人的 SMTP 配置");
        }

        smtpConfigRepository.clearDefault(userId);
        config.setDefault(true);
        smtpConfigRepository.save(config);
        return ApiResponse.successWithoutData();
    }

    private SmtpConfigDto.SmtpConfigResponse toResponse(SmtpConfig config) {
        return new SmtpConfigDto.SmtpConfigResponse(
                config.getId(),
                config.getName(),
                config.getHost(),
                config.getPort(),
                config.getUsername(),
                config.getFromAddress(),
                config.getImapHost(),
                config.getImapPort(),
                config.isSslEnabled(),
                config.isStarttlsEnabled(),
                config.isDefault(),
                config.getCreatedBy()
        );
    }

    private JavaMailSenderImpl createSender(SmtpConfig config, String password) {
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
}
