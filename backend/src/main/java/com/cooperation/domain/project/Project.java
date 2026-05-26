package com.cooperation.domain.project;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 项目聚合根，维护项目状态以及结束、重新打开和写操作校验规则。
 */
public final class Project {

    private final Long id;
    private final Long groupId;
    private final Long ownerId;
    private final String name;
    private ProjectStatus status;
    private LocalDateTime endedAt;
    private LocalDateTime reopenedAt;

    private Project(Long id, Long groupId, Long ownerId, String name, ProjectStatus status) {
        this.id = id;
        this.groupId = Objects.requireNonNull(groupId, "项目所属小组不能为空");
        this.ownerId = Objects.requireNonNull(ownerId, "项目负责人不能为空");
        this.name = validateName(name);
        this.status = Objects.requireNonNull(status, "项目状态不能为空");
    }

    /**
     * 创建默认处于协作中状态的新项目。
     *
     * @param groupId 项目所属小组标识。
     * @param ownerId 项目负责人用户标识。
     * @param name 项目名称。
     * @return 新建项目聚合。
     */
    public static Project create(Long groupId, Long ownerId, String name) {
        return new Project(null, groupId, ownerId, name, ProjectStatus.ACTIVE);
    }

    /**
     * 从仓储数据重建项目聚合，不绑定具体持久化框架。
     *
     * @param id 项目唯一标识。
     * @param groupId 项目所属小组标识。
     * @param ownerId 项目负责人用户标识。
     * @param name 项目名称。
     * @param status 当前项目状态。
     * @return 重建后的项目聚合。
     */
    public static Project restore(Long id, Long groupId, Long ownerId, String name, ProjectStatus status) {
        return new Project(Objects.requireNonNull(id, "项目标识不能为空"), groupId, ownerId, name, status);
    }

    /**
     * 结束项目并锁定后续写操作。
     *
     * @param operatorId 执行结束操作的用户标识。
     */
    public void end(Long operatorId) {
        checkOwner(operatorId);
        if (status == ProjectStatus.ENDED) {
            throw new IllegalStateException("项目已结束，不能重复结束");
        }
        status = ProjectStatus.ENDED;
        endedAt = LocalDateTime.now();
    }

    /**
     * 重新打开已结束项目，恢复协作写操作。
     *
     * @param operatorId 执行重新打开操作的用户标识。
     */
    public void reopen(Long operatorId) {
        checkOwner(operatorId);
        if (status == ProjectStatus.ACTIVE) {
            throw new IllegalStateException("项目已处于协作中状态");
        }
        status = ProjectStatus.ACTIVE;
        reopenedAt = LocalDateTime.now();
    }

    /**
     * 判断当前项目是否允许写操作。
     *
     * @return 协作中返回 true，已结束返回 false。
     */
    public boolean canWrite() {
        return status == ProjectStatus.ACTIVE;
    }

    /**
     * 校验项目是否允许写操作，已结束时抛出异常。
     */
    public void checkWritable() {
        if (!canWrite()) {
            throw new IllegalStateException("项目已结束，不能执行写操作");
        }
    }

    /**
     * 获取项目唯一标识。
     *
     * @return 项目唯一标识，新建未保存项目为空。
     */
    public Long getId() {
        return id;
    }

    /**
     * 获取项目所属小组标识。
     *
     * @return 小组标识。
     */
    public Long getGroupId() {
        return groupId;
    }

    /**
     * 获取项目负责人用户标识。
     *
     * @return 负责人用户标识。
     */
    public Long getOwnerId() {
        return ownerId;
    }

    /**
     * 获取项目名称。
     *
     * @return 项目名称。
     */
    public String getName() {
        return name;
    }

    /**
     * 获取当前项目状态。
     *
     * @return 项目状态。
     */
    public ProjectStatus getStatus() {
        return status;
    }

    /**
     * 获取最近一次结束时间。
     *
     * @return 结束时间，未结束过时为空。
     */
    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    /**
     * 获取最近一次重新打开时间。
     *
     * @return 重新打开时间，未重新打开过时为空。
     */
    public LocalDateTime getReopenedAt() {
        return reopenedAt;
    }

    private void checkOwner(Long operatorId) {
        if (!ownerId.equals(operatorId)) {
            throw new IllegalArgumentException("只有项目负责人可以执行该操作");
        }
    }

    private static String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("项目名称不能为空");
        }
        return name;
    }
}
