-- 新增 SMTP 邮件配置表，支持多套 SMTP 服务配置和动态切换。

CREATE TABLE smtp_configs (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '配置标识',
    name VARCHAR(100) NOT NULL COMMENT '配置名称（如：公司邮箱、QQ邮箱）',
    host VARCHAR(255) NOT NULL COMMENT 'SMTP 服务器地址',
    port INT NOT NULL DEFAULT 465 COMMENT 'SMTP 端口',
    username VARCHAR(255) NOT NULL COMMENT 'SMTP 登录账号',
    password VARCHAR(512) NOT NULL COMMENT 'SMTP 登录密码（AES 加密存储）',
    from_address VARCHAR(255) NOT NULL COMMENT '发件人地址',
    ssl_enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用 SSL',
    starttls_enabled BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否启用 STARTTLS',
    is_default BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否为默认配置',
    created_by BIGINT UNSIGNED NOT NULL COMMENT '创建人用户标识',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_smtp_configs_name_user (name, created_by),
    KEY idx_smtp_configs_created_by (created_by),
    CONSTRAINT fk_smtp_configs_created_by FOREIGN KEY (created_by) REFERENCES users (id)
);

-- mail_drafts 新增 smtp_config_id 列，记录发送时选用的 SMTP 配置。
ALTER TABLE mail_drafts
    ADD COLUMN smtp_config_id BIGINT UNSIGNED NULL COMMENT '发送时使用的 SMTP 配置标识' AFTER package_id,
    ADD KEY idx_mail_drafts_smtp_config_id (smtp_config_id),
    ADD CONSTRAINT fk_mail_drafts_smtp_config_id FOREIGN KEY (smtp_config_id) REFERENCES smtp_configs (id) ON DELETE SET NULL;
