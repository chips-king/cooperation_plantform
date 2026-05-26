package com.cooperation.infrastructure.persistence.project;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Getter;
import lombok.Setter;

/**
 * 项目持久化实体，映射数据库 {@code projects} 表的列结构。
 */
@Getter
@Setter
@TableName("projects")
public class ProjectEntity {

    /**
     * 项目唯一标识，对应 {@code BIGINT UNSIGNED AUTO_INCREMENT} 主键。
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 所属小组标识，对应 {@code group_id} 外键列。
     */
    @TableField("group_id")
    private Long groupId;

    /**
     * 项目名称，对应 {@code name} 文本列。
     */
    @TableField("name")
    private String name;

    /**
     * 项目状态，对应 {@code status} 文本列。
     */
    @TableField("status")
    private String status;

    /**
     * 最近一次最终压缩包标识，对应 {@code latest_package_id} 可空列。
     */
    @TableField("latest_package_id")
    private String latestPackageId;

    /**
     * 项目结束时间，对应 {@code ended_at} 可空时间列。
     */
    @TableField("ended_at")
    private LocalDateTime endedAt;

    /**
     * 最近重新打开时间，对应 {@code reopened_at} 可空时间列。
     */
    @TableField("reopened_at")
    private LocalDateTime reopenedAt;

    /**
     * 最近更新时间，对应 {@code updated_at} 自动更新时间列。
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
