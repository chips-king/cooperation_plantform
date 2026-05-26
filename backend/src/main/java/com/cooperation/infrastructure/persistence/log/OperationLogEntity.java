package com.cooperation.infrastructure.persistence.log;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 操作记录持久化实体，映射数据库 operation_logs 表。
 */
@Getter
@Setter
@TableName("operation_logs")
public class OperationLogEntity {

    /**
     * 记录标识，对应自增主键。
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 所属项目标识。
     */
    @TableField("project_id")
    private Long projectId;

    /**
     * 操作人用户标识。
     */
    @TableField("actor_id")
    private Long actorId;

    /**
     * 操作类型。
     */
    @TableField("action")
    private String action;

    /**
     * 目标类型。
     */
    @TableField("target_type")
    private String targetType;

    /**
     * 目标标识。
     */
    @TableField("target_id")
    private String targetId;

    /**
     * 可读摘要。
     */
    @TableField("summary")
    private String summary;

    /**
     * 结构化细节 JSON，当前先以字符串承载。
     */
    @TableField("metadata")
    private String metadata;

    /**
     * 操作时间。
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * 项目结束后保留到期时间。
     */
    @TableField("retain_until")
    private LocalDateTime retainUntil;
}
