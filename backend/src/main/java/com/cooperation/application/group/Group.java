package com.cooperation.application.group;

import java.util.Objects;

/**
 * 小组应用层实体，承载创建小组用例需要的负责人、名称和状态信息。
 */
public final class Group {

    private final Long id;
    private final Long ownerId;
    private final String name;
    private final Status status;

    private Group(Long id, Long ownerId, String name, Status status) {
        this.id = id;
        this.ownerId = Objects.requireNonNull(ownerId, "小组负责人不能为空");
        this.name = validateName(name);
        this.status = Objects.requireNonNull(status, "小组状态不能为空");
    }

    /**
     * 创建默认启用的新小组。
     *
     * @param ownerId 负责人用户标识。
     * @param name 小组名称。
     * @return 新建小组实体。
     */
    public static Group create(Long ownerId, String name) {
        return new Group(null, ownerId, name, Status.ACTIVE);
    }

    /**
     * 从仓储数据恢复小组实体。
     *
     * @param id 小组标识。
     * @param ownerId 负责人用户标识。
     * @param name 小组名称。
     * @param status 小组状态。
     * @return 恢复后的小组实体。
     */
    public static Group restore(Long id, Long ownerId, String name, Status status) {
        return new Group(Objects.requireNonNull(id, "小组标识不能为空"), ownerId, name, status);
    }

    /**
     * 获取小组标识。
     *
     * @return 小组标识，新建未保存时为空。
     */
    public Long getId() {
        return id;
    }

    /**
     * 获取负责人用户标识。
     *
     * @return 负责人用户标识。
     */
    public Long getOwnerId() {
        return ownerId;
    }

    /**
     * 获取小组名称。
     *
     * @return 小组名称。
     */
    public String getName() {
        return name;
    }

    /**
     * 获取小组状态。
     *
     * @return 小组状态。
     */
    public Status getStatus() {
        return status;
    }

    private static String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("小组名称不能为空");
        }
        return name;
    }

    /**
     * 小组状态枚举。
     */
    public enum Status {
        /** 小组正常可用。 */
        ACTIVE
    }
}
