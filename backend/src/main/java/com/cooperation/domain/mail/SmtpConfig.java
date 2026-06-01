package com.cooperation.domain.mail;

import java.time.Instant;
import java.util.Objects;

/**
 * SMTP 邮件配置领域实体，封装一套 SMTP 发送参数。
 */
public class SmtpConfig {

    private Long id;
    private final String name;
    private final String host;
    private final int port;
    private final String username;
    private String password;
    private final String fromAddress;
    private final String imapHost;
    private final int imapPort;
    private final boolean sslEnabled;
    private final boolean starttlsEnabled;
    private boolean isDefault;
    private final Long createdBy;
    private final Instant createdAt;
    private Instant updatedAt;

    /**
     * 创建 SMTP 配置实体。
     */
    public SmtpConfig(Long id, String name, String host, int port, String username, String password,
                      String fromAddress, String imapHost, int imapPort,
                      boolean sslEnabled, boolean starttlsEnabled,
                      boolean isDefault, Long createdBy, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "配置名称不能为空");
        this.host = Objects.requireNonNull(host, "SMTP 服务器地址不能为空");
        this.port = port;
        this.username = Objects.requireNonNull(username, "SMTP 账号不能为空");
        this.password = password;
        this.fromAddress = Objects.requireNonNull(fromAddress, "发件人地址不能为空");
        this.imapHost = imapHost == null ? "" : imapHost;
        this.imapPort = imapPort;
        this.sslEnabled = sslEnabled;
        this.starttlsEnabled = starttlsEnabled;
        this.isDefault = isDefault;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 创建新的 SMTP 配置。
     */
    public static SmtpConfig create(String name, String host, int port, String username, String password,
                                    String fromAddress, String imapHost, int imapPort,
                                    boolean sslEnabled, boolean starttlsEnabled,
                                    boolean isDefault, Long createdBy) {
        return new SmtpConfig(null, name, host, port, username, password,
                fromAddress, imapHost, imapPort, sslEnabled, starttlsEnabled, isDefault, createdBy, null, null);
    }

    public SmtpConfig withId(Long id) {
        this.id = id;
        return this;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getHost() { return host; }
    public int getPort() { return port; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getFromAddress() { return fromAddress; }
    public String getImapHost() { return imapHost; }
    public int getImapPort() { return imapPort; }
    public boolean isSslEnabled() { return sslEnabled; }
    public boolean isStarttlsEnabled() { return starttlsEnabled; }
    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean isDefault) { this.isDefault = isDefault; }
    public Long getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    /**
     * 校验 SMTP 配置完整性。
     */
    public void validate() {
        if (host.isBlank() || port <= 0 || username.isBlank() || password.isBlank() || fromAddress.isBlank()) {
            throw new IllegalStateException("SMTP 配置不完整，请检查服务器地址、端口、账号、密码和发件人地址");
        }
    }
}
