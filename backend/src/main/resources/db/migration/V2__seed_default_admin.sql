-- 写入本地与首版部署可用的默认管理员用户，保证硬编码开发登录用户能满足外键约束。

INSERT INTO users (id, display_name, email, status)
VALUES (1, '管理员', 'admin@example.com', 'active')
ON DUPLICATE KEY UPDATE
    display_name = VALUES(display_name),
    status = VALUES(status);
