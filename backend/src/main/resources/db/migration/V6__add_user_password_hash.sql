-- 为 users 表添加密码哈希字段，支持后续的数据库认证升级。
-- 默认管理员账户的初始密码为 "123456"（BCrypt 哈希）。

ALTER TABLE users
    ADD COLUMN password_hash VARCHAR(255) NULL COMMENT '用户密码的 BCrypt 哈希值' AFTER status;

-- 为已有的管理员账户设置初始密码 "123456" 的 BCrypt 哈希
UPDATE users SET password_hash = '$2a$10$Mz2EAGS5kmFTLW.Ouox0BOlnuSiyo6jAumzLYizQgobSKFrpJ/PDW'
WHERE id = 1 AND password_hash IS NULL;
