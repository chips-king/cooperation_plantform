## SMTP 邮件配置管理设计文档

### 背景与目标

项目已有完整的邮件草稿功能（创建、编辑、发送），SMTP 发送代码也已实现（`SmtpMailDraftSender`）。但 SMTP 参数写在 `application.yml` 环境变量中（`MAIL_HOST`、`MAIL_PORT` 等），默认关闭，修改需重启服务。

本设计将 SMTP 配置改为数据库存储 + 界面管理，支持多套配置、动态切换、默认配置，用户可在个人中心管理 SMTP 服务。

### 数据库设计

#### 新建 `smtp_configs` 表（Flyway V8 迁移）

| 列名 | 类型 | 说明 |
|------|------|------|
| id | BIGINT UNSIGNED AUTO_INCREMENT | 主键 |
| name | VARCHAR(100) NOT NULL | 配置名称（如"公司邮箱"） |
| host | VARCHAR(255) NOT NULL | SMTP 服务器地址 |
| port | INT NOT NULL DEFAULT 465 | SMTP 端口 |
| username | VARCHAR(255) NOT NULL | 登录账号 |
| password | VARCHAR(512) NOT NULL | 登录密码（AES 加密） |
| from_address | VARCHAR(255) NOT NULL | 发件人地址 |
| ssl_enabled | BOOLEAN NOT NULL DEFAULT TRUE | 是否启用 SSL |
| starttls_enabled | BOOLEAN NOT NULL DEFAULT FALSE | 是否启用 STARTTLS |
| is_default | BOOLEAN NOT NULL DEFAULT FALSE | 是否为默认配置 |
| created_by | BIGINT UNSIGNED NOT NULL | 创建人（FK users.id） |
| created_at | DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) | 创建时间 |
| updated_at | DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE | 更新时间 |

约束：同一用户（created_by）下 name 唯一。

#### `mail_drafts` 表变更

新增列 `smtp_config_id BIGINT UNSIGNED NULL`（FK smtp_configs.id），记录发送时选用的 SMTP 配置。可为 NULL 表示使用默认配置。

### 后端设计

#### 分层结构

```
domain/mail/SmtpConfig.java          — 领域实体
application/mail/SmtpConfigRepository.java — 仓储端口接口
application/mail/SmtpConfigUseCases.java   — CRUD + 测试 + 设默认
infrastructure/persistence/JdbcSmtpConfigRepository.java — JDBC 实现
web/mail/SmtpConfigController.java   — REST 控制器
web/mail/SmtpConfigDto.java          — 请求/响应 DTO
infrastructure/mail/SmtpPasswordEncryptor.java — AES 密码加解密
```

#### API 接口

| 方法 | 路径 | 功能 |
|------|------|------|
| GET | /smtp-configs | 查询当前用户所有 SMTP 配置 |
| POST | /smtp-configs | 新增配置 |
| PUT | /smtp-configs/{id} | 修改配置 |
| DELETE | /smtp-configs/{id} | 删除配置 |
| POST | /smtp-configs/{id}/test | 发送测试邮件（请求体含 testRecipient） |
| PUT | /smtp-configs/{id}/default | 设为默认配置 |

#### 密码安全

- 传输：前端以明文传输（HTTPS 在生产环境保障传输安全，开发环境暂不加密）
- 存储：后端使用 AES-256 对称加密，密钥从 `application.yml` 的 `app.security.aes-key` 读取
- 查询响应：密码字段脱敏，仅返回 `****`

#### 发送流程变更

`SendMailDraftUseCase.Command` 新增可选字段 `smtpConfigId`。`SmtpMailDraftSender` 改为接受 `SmtpConfigRepository` 注入，发送时：
1. 如果指定了 `smtpConfigId`，从数据库加载该配置
2. 否则加载 `is_default = true` 的配置
3. 如果没有默认配置，抛出异常提示用户先配置 SMTP
4. 用加载的配置动态创建 `JavaMailSenderImpl` 发送

### 前端设计

#### 个人中心新增「邮件设置」Tab

在 `UserProfilePage.vue` 新增 Tab 页「邮件设置」：

- **配置列表**：卡片/表格形式展示所有 SMTP 配置，显示名称、服务器地址、端口、发件人、默认标识
- **新增/编辑对话框**：表单包含名称、host、port（默认 465）、账号、密码、发件人地址、SSL 开关
- **删除**：确认弹窗后删除
- **测试连接**：输入测试收件人地址，点击后发送测试邮件，显示成功/失败结果
- **设为默认**：点击后该配置变为默认，其他配置取消默认

#### 邮件草稿发送变更

`MailDraftPage.vue` 的发送确认弹窗增加「发送邮箱」下拉选择框，列出所有 SMTP 配置（显示名称 + 发件人），默认选中 `isDefault` 的配置。选中值作为 `smtpConfigId` 随发送请求一起提交。

### 不在范围内

- 第三方邮箱草稿箱 API（QQ/163）集成
- 发送失败自动重试
- SMTP 配置的权限分级（当前所有用户均可管理自己的配置）
- 密码的 RSA 前端加密（留到生产环境加固时做）
