-- 为 file_assets 表添加上传时间字段，用于在文件列表中展示最后上传时间。
ALTER TABLE file_assets
    ADD COLUMN uploaded_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '上传时间' AFTER uploaded_by;
