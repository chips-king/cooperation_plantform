-- 为 users 表添加登录用户名字段，支持独立于展示名称的登录账号。

ALTER TABLE users
    ADD COLUMN username VARCHAR(50) NULL COMMENT '登录用户名' AFTER email,
    ADD UNIQUE KEY uk_users_username (username);

-- 为已有的管理员账户设置登录用户名
UPDATE users SET username = 'admin' WHERE id = 1 AND username IS NULL;
