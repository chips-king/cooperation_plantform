package com.cooperation.web.mail;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * SMTP 邮件配置 Web API 数据传输对象集合。
 */
public final class SmtpConfigDto {

    private SmtpConfigDto() {
    }

    /**
     * 创建或修改 SMTP 配置请求。
     */
    public record SaveSmtpConfigRequest(
            @NotBlank(message = "配置名称不能为空") String name,
            @NotBlank(message = "SMTP 服务器地址不能为空") String host,
            @NotNull(message = "端口不能为空") @Min(value = 1, message = "端口最小为 1") @Max(value = 65535, message = "端口最大为 65535") Integer port,
            @NotBlank(message = "登录账号不能为空") String username,
            String password,
            @NotBlank(message = "发件人地址不能为空") String fromAddress,
            String imapHost,
            Integer imapPort,
            Boolean sslEnabled,
            Boolean starttlsEnabled,
            Boolean isDefault
    ) {
    }

    /**
     * 发送测试邮件请求。
     */
    public record TestSmtpRequest(
            @NotBlank(message = "测试收件人不能为空") String testRecipient
    ) {
    }

    /**
     * SMTP 配置响应（密码脱敏）。
     */
    public record SmtpConfigResponse(
            Long id,
            String name,
            String host,
            int port,
            String username,
            String fromAddress,
            String imapHost,
            int imapPort,
            boolean sslEnabled,
            boolean starttlsEnabled,
            boolean isDefault,
            Long createdBy
    ) {
    }

    /**
     * 测试邮件发送结果响应。
     */
    public record TestSmtpResponse(
            boolean success,
            String message
    ) {
    }
}
