-- 初始化核心业务表结构，覆盖用户、小组、项目、协作文件与通知记录。

CREATE TABLE users (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户唯一标识',
    display_name VARCHAR(100) NOT NULL COMMENT '用户展示名称',
    email VARCHAR(255) NOT NULL COMMENT '用户邮箱，可用于登录或展示',
    status VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT '用户状态：active 正常，disabled 禁用',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email),
    KEY idx_users_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';

CREATE TABLE user_groups (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '小组唯一标识',
    name VARCHAR(100) NOT NULL COMMENT '小组名称',
    owner_id BIGINT UNSIGNED NOT NULL COMMENT '默认负责人用户标识',
    status VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT '小组状态：active 正常',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_user_groups_owner_id (owner_id),
    KEY idx_user_groups_status (status),
    CONSTRAINT fk_user_groups_owner_id FOREIGN KEY (owner_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='小组表';

CREATE TABLE projects (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '项目唯一标识',
    group_id BIGINT UNSIGNED NOT NULL COMMENT '所属小组标识',
    name VARCHAR(150) NOT NULL COMMENT '项目名称',
    status VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT '项目状态：active 协作中，ended 已结束',
    latest_package_id VARCHAR(64) NULL COMMENT '最近一次最终压缩包标识',
    ended_at DATETIME(3) NULL COMMENT '项目结束时间',
    reopened_at DATETIME(3) NULL COMMENT '最近重新打开时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最近更新时间',
    PRIMARY KEY (id),
    KEY idx_projects_group_id (group_id),
    KEY idx_projects_status (status),
    KEY idx_projects_latest_package_id (latest_package_id),
    UNIQUE KEY uk_projects_group_name (group_id, name),
    CONSTRAINT fk_projects_group_id FOREIGN KEY (group_id) REFERENCES user_groups (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目表';

CREATE TABLE memberships (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '成员关系标识',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户标识',
    group_id BIGINT UNSIGNED NOT NULL COMMENT '小组标识',
    project_id BIGINT UNSIGNED NULL COMMENT '项目标识，为空表示小组级成员',
    project_scope_id BIGINT UNSIGNED GENERATED ALWAYS AS (COALESCE(project_id, 0)) STORED COMMENT '唯一约束使用的项目范围标识',
    role_template VARCHAR(32) NOT NULL COMMENT '角色模板：OWNER 负责人，MEMBER 成员，READ_ONLY 只读',
    custom_permissions JSON NULL COMMENT '模板基础上的权限覆盖',
    status VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT '成员状态：active 正常，pending 待审核，removed 已移除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_memberships_scope_user (user_id, group_id, project_scope_id),
    KEY idx_memberships_group_id (group_id),
    KEY idx_memberships_project_id (project_id),
    KEY idx_memberships_status (status),
    CONSTRAINT fk_memberships_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_memberships_group_id FOREIGN KEY (group_id) REFERENCES user_groups (id),
    CONSTRAINT fk_memberships_project_id FOREIGN KEY (project_id) REFERENCES projects (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='成员关系表';

CREATE TABLE invitations (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '邀请标识',
    group_id BIGINT UNSIGNED NOT NULL COMMENT '所属小组标识',
    project_id BIGINT UNSIGNED NULL COMMENT '目标项目标识，可为空',
    mode VARCHAR(32) NOT NULL COMMENT '邀请模式：direct 直接加入，review_required 需要审核',
    code VARCHAR(120) NOT NULL COMMENT '邀请码或链接标识',
    status VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT '邀请状态：active 有效，disabled 失效',
    created_by BIGINT UNSIGNED NOT NULL COMMENT '创建人用户标识',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_invitations_code (code),
    KEY idx_invitations_group_id (group_id),
    KEY idx_invitations_project_id (project_id),
    KEY idx_invitations_created_by (created_by),
    KEY idx_invitations_status (status),
    CONSTRAINT fk_invitations_group_id FOREIGN KEY (group_id) REFERENCES user_groups (id),
    CONSTRAINT fk_invitations_project_id FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_invitations_created_by FOREIGN KEY (created_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='邀请表';

CREATE TABLE directories (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '目录标识',
    project_id BIGINT UNSIGNED NOT NULL COMMENT '所属项目标识',
    parent_id BIGINT UNSIGNED NULL COMMENT '父目录标识，根目录为空',
    parent_scope_id BIGINT UNSIGNED GENERATED ALWAYS AS (COALESCE(parent_id, 0)) STORED COMMENT '唯一约束使用的父目录范围标识',
    name VARCHAR(255) NOT NULL COMMENT '目录名',
    status VARCHAR(32) NOT NULL DEFAULT 'not_started' COMMENT '目录状态：not_started 未开始，in_progress 进行中，completed 已完成',
    created_by BIGINT UNSIGNED NOT NULL COMMENT '创建人用户标识',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_directories_parent_name (project_id, parent_scope_id, name),
    KEY idx_directories_project_id (project_id),
    KEY idx_directories_parent_id (parent_id),
    KEY idx_directories_created_by (created_by),
    KEY idx_directories_status (status),
    CONSTRAINT fk_directories_project_id FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_directories_parent_id FOREIGN KEY (parent_id) REFERENCES directories (id),
    CONSTRAINT fk_directories_created_by FOREIGN KEY (created_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='目录表';

CREATE TABLE file_assets (
    id VARCHAR(64) NOT NULL COMMENT '文件标识',
    project_id BIGINT UNSIGNED NOT NULL COMMENT '所属项目标识',
    directory_id BIGINT UNSIGNED NOT NULL COMMENT '所属目录标识',
    name VARCHAR(255) NOT NULL COMMENT '文件名',
    size BIGINT UNSIGNED NOT NULL COMMENT '文件大小，单位字节',
    mime_type VARCHAR(150) NULL COMMENT '文件 MIME 类型',
    extension VARCHAR(32) NULL COMMENT '文件扩展名',
    storage_key VARCHAR(512) NOT NULL COMMENT '内部存储位置',
    uploaded_by BIGINT UNSIGNED NOT NULL COMMENT '上传人用户标识',
    version_group_id VARCHAR(64) NOT NULL COMMENT '同名版本组标识',
    version_no INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '版本号，从 1 开始递增',
    status VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT '文件状态：active 正常，trashed 回收站，superseded 被覆盖',
    deleted_at DATETIME(3) NULL COMMENT '删除时间',
    deleted_by BIGINT UNSIGNED NULL COMMENT '删除人用户标识',
    PRIMARY KEY (id),
    UNIQUE KEY uk_file_assets_storage_key (storage_key),
    UNIQUE KEY uk_file_assets_version (version_group_id, version_no),
    KEY idx_file_assets_project_directory (project_id, directory_id),
    KEY idx_file_assets_directory_name_status (directory_id, name, status),
    KEY idx_file_assets_uploaded_by (uploaded_by),
    KEY idx_file_assets_deleted_by (deleted_by),
    KEY idx_file_assets_status (status),
    CONSTRAINT fk_file_assets_project_id FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_file_assets_directory_id FOREIGN KEY (directory_id) REFERENCES directories (id),
    CONSTRAINT fk_file_assets_uploaded_by FOREIGN KEY (uploaded_by) REFERENCES users (id),
    CONSTRAINT fk_file_assets_deleted_by FOREIGN KEY (deleted_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件资产表';

CREATE TABLE package_artifacts (
    id VARCHAR(64) NOT NULL COMMENT '压缩包标识',
    project_id BIGINT UNSIGNED NOT NULL COMMENT '所属项目标识',
    filename VARCHAR(255) NOT NULL COMMENT '负责人填写的文件名',
    format VARCHAR(32) NOT NULL COMMENT '压缩格式：zip、7z、tar.gz',
    storage_key VARCHAR(512) NOT NULL COMMENT '内部存储位置',
    size BIGINT UNSIGNED NOT NULL COMMENT '文件大小，单位字节',
    created_by BIGINT UNSIGNED NOT NULL COMMENT '创建人用户标识',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    snapshot_created_at DATETIME(3) NOT NULL COMMENT '打包所使用文件树快照时间',
    is_latest TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否最近一次压缩包',
    PRIMARY KEY (id),
    UNIQUE KEY uk_package_artifacts_storage_key (storage_key),
    KEY idx_package_artifacts_project_latest (project_id, is_latest),
    KEY idx_package_artifacts_created_by (created_by),
    CONSTRAINT fk_package_artifacts_project_id FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_package_artifacts_created_by FOREIGN KEY (created_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='最终压缩包表';

CREATE TABLE mail_drafts (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '草稿标识',
    project_id BIGINT UNSIGNED NOT NULL COMMENT '所属项目标识',
    recipient VARCHAR(255) NOT NULL COMMENT '收件人',
    subject VARCHAR(255) NOT NULL COMMENT '邮件主题',
    body TEXT NOT NULL COMMENT '邮件正文',
    package_id VARCHAR(64) NOT NULL COMMENT '附件压缩包标识',
    attachment_filename VARCHAR(255) NOT NULL COMMENT '附件展示文件名',
    status VARCHAR(32) NOT NULL DEFAULT 'draft' COMMENT '草稿状态：draft 草稿，sent 已发送，cancelled 已取消',
    created_by BIGINT UNSIGNED NOT NULL COMMENT '创建人用户标识',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    sent_at DATETIME(3) NULL COMMENT '发送时间',
    PRIMARY KEY (id),
    KEY idx_mail_drafts_project_id (project_id),
    KEY idx_mail_drafts_package_id (package_id),
    KEY idx_mail_drafts_created_by (created_by),
    KEY idx_mail_drafts_status (status),
    CONSTRAINT fk_mail_drafts_project_id FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_mail_drafts_package_id FOREIGN KEY (package_id) REFERENCES package_artifacts (id),
    CONSTRAINT fk_mail_drafts_created_by FOREIGN KEY (created_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='邮件草稿表';

CREATE TABLE operation_logs (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '记录标识',
    project_id BIGINT UNSIGNED NULL COMMENT '所属项目标识；小组级操作可为空',
    actor_id BIGINT UNSIGNED NOT NULL COMMENT '操作人用户标识',
    action VARCHAR(64) NOT NULL COMMENT '操作类型',
    target_type VARCHAR(64) NOT NULL COMMENT '目标类型',
    target_id VARCHAR(64) NOT NULL COMMENT '目标标识',
    summary VARCHAR(500) NOT NULL COMMENT '可读摘要',
    metadata JSON NULL COMMENT '结构化细节',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '操作时间',
    retain_until DATETIME(3) NULL COMMENT '项目结束后保留到期时间',
    PRIMARY KEY (id),
    KEY idx_operation_logs_project_created (project_id, created_at),
    KEY idx_operation_logs_actor_id (actor_id),
    KEY idx_operation_logs_action (action),
    KEY idx_operation_logs_target (target_type, target_id),
    KEY idx_operation_logs_retain_until (retain_until),
    CONSTRAINT fk_operation_logs_project_id FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_operation_logs_actor_id FOREIGN KEY (actor_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='操作记录表';

CREATE TABLE notifications (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '通知标识',
    project_id BIGINT UNSIGNED NOT NULL COMMENT '所属项目标识',
    recipient_id BIGINT UNSIGNED NOT NULL COMMENT '接收人用户标识',
    type VARCHAR(64) NOT NULL COMMENT '通知类型',
    title VARCHAR(150) NOT NULL COMMENT '标题',
    content VARCHAR(1000) NOT NULL COMMENT '内容',
    read_at DATETIME(3) NULL COMMENT '阅读时间',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_notifications_recipient_read (recipient_id, read_at),
    KEY idx_notifications_project_id (project_id),
    KEY idx_notifications_type (type),
    KEY idx_notifications_created_at (created_at),
    CONSTRAINT fk_notifications_project_id FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_notifications_recipient_id FOREIGN KEY (recipient_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知表';
