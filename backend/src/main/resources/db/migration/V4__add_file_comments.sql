-- 文件评论表，支持小组成员对每个文件进行讨论。
CREATE TABLE file_comments (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '评论标识',
    file_id VARCHAR(64) NOT NULL COMMENT '所属文件标识',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '评论人用户标识',
    content TEXT NOT NULL COMMENT '评论内容',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_file_comments_file_id (file_id),
    KEY idx_file_comments_user_id (user_id),
    CONSTRAINT fk_file_comments_file_id FOREIGN KEY (file_id) REFERENCES file_assets (id) ON DELETE CASCADE,
    CONSTRAINT fk_file_comments_user_id FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件评论表';
