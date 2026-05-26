package com.cooperation.infrastructure.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * SMTP 邮件配置，所有敏感字段均从环境变量或私有配置注入。
 */
@Component
@ConfigurationProperties(prefix = "app.mail")
public class MailSmtpProperties {

    private boolean enabled;
    private String host = "";
    private int port = 465;
    private String username = "";
    private String password = "";
    private String from = "";
    private boolean sslEnabled = true;
    private boolean starttlsEnabled;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host == null ? "" : host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username == null ? "" : username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password == null ? "" : password;
    }

    public String getFrom() {
        return from == null || from.isBlank() ? username : from;
    }

    public void setFrom(String from) {
        this.from = from == null ? "" : from;
    }

    public boolean isSslEnabled() {
        return sslEnabled;
    }

    public void setSslEnabled(boolean sslEnabled) {
        this.sslEnabled = sslEnabled;
    }

    public boolean isStarttlsEnabled() {
        return starttlsEnabled;
    }

    public void setStarttlsEnabled(boolean starttlsEnabled) {
        this.starttlsEnabled = starttlsEnabled;
    }

    /**
     * 校验 SMTP 发送所需配置是否完整。
     */
    public void validateEnabled() {
        if (!enabled) {
            throw new IllegalStateException("邮箱服务未配置，无法发送邮件，请先配置邮箱服务或人工下载附件发送");
        }
        if (host.isBlank() || port <= 0 || username.isBlank() || password.isBlank() || getFrom().isBlank()) {
            throw new IllegalStateException("邮箱服务配置不完整，请检查 MAIL_HOST、MAIL_PORT、MAIL_USERNAME、MAIL_PASSWORD、MAIL_FROM");
        }
    }
}
