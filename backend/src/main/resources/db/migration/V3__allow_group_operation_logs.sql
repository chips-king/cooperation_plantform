ALTER TABLE operation_logs
    DROP FOREIGN KEY fk_operation_logs_project_id;

ALTER TABLE operation_logs
    MODIFY project_id BIGINT UNSIGNED NULL COMMENT '所属项目标识；小组级操作可为空';

ALTER TABLE operation_logs
    ADD CONSTRAINT fk_operation_logs_project_id
        FOREIGN KEY (project_id) REFERENCES projects(id);
