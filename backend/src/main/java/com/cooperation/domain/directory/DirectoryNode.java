package com.cooperation.domain.directory;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 目录实体，维护目录归属、父目录和任务状态变更规则。
 */
public final class DirectoryNode {

    private final Long projectId;
    private final Long parentId;
    private final String name;
    private final Long createdBy;
    private DirectoryStatus status;
    private LocalDateTime statusChangedAt;
    private Long statusChangedBy;

    private DirectoryNode(Long projectId, Long parentId, String name, Long createdBy) {
        this.projectId = Objects.requireNonNull(projectId, "目录所属项目不能为空");
        this.parentId = parentId;
        this.name = validateName(name);
        this.createdBy = Objects.requireNonNull(createdBy, "目录创建人不能为空");
        this.status = DirectoryStatus.NOT_STARTED;
    }

    /**
     * 创建默认处于未开始状态的新目录。
     *
     * @param projectId 目录所属项目标识。
     * @param parentId 父目录标识，根目录为空。
     * @param name 目录名称。
     * @param createdBy 创建人用户标识。
     * @return 新建目录实体。
     */
    public static DirectoryNode create(Long projectId, Long parentId, String name, Long createdBy) {
        return new DirectoryNode(projectId, parentId, name, createdBy);
    }

    /**
     * 变更目录任务状态，未开始目录必须先进入进行中后才能完成。
     *
     * @param nextStatus 目标目录状态。
     * @param operatorId 执行状态变更的用户标识。
     */
    public void changeStatus(DirectoryStatus nextStatus, Long operatorId) {
        Objects.requireNonNull(nextStatus, "目标目录状态不能为空");
        Objects.requireNonNull(operatorId, "状态变更操作人不能为空");
        if (status == DirectoryStatus.NOT_STARTED && nextStatus == DirectoryStatus.COMPLETED) {
            throw new IllegalStateException("未开始目录不能直接标记为已完成");
        }
        status = nextStatus;
        statusChangedBy = operatorId;
        statusChangedAt = LocalDateTime.now();
    }

    /**
     * 获取目录所属项目标识。
     *
     * @return 项目标识。
     */
    public Long getProjectId() {
        return projectId;
    }

    /**
     * 获取父目录标识。
     *
     * @return 父目录标识，根目录为空。
     */
    public Long getParentId() {
        return parentId;
    }

    /**
     * 获取目录名称。
     *
     * @return 目录名称。
     */
    public String getName() {
        return name;
    }

    /**
     * 获取目录创建人用户标识。
     *
     * @return 创建人用户标识。
     */
    public Long getCreatedBy() {
        return createdBy;
    }

    /**
     * 获取当前目录任务状态。
     *
     * @return 目录任务状态。
     */
    public DirectoryStatus getStatus() {
        return status;
    }

    /**
     * 获取最近一次状态变更时间。
     *
     * @return 状态变更时间，未变更过时为空。
     */
    public LocalDateTime getStatusChangedAt() {
        return statusChangedAt;
    }

    /**
     * 获取最近一次状态变更操作人。
     *
     * @return 操作人用户标识，未变更过时为空。
     */
    public Long getStatusChangedBy() {
        return statusChangedBy;
    }

    private static String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("目录名称不能为空");
        }
        return name;
    }
}
