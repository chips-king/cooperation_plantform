-- 为 smtp_configs 表添加 IMAP 配置字段，支持发送后将邮件写入已发送文件夹。

ALTER TABLE smtp_configs
    ADD COLUMN imap_host VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'IMAP 服务器地址' AFTER from_address,
    ADD COLUMN imap_port INT NOT NULL DEFAULT 993 COMMENT 'IMAP 端口' AFTER imap_host;
