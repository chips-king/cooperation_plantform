package com.cooperation.domain.project;

/**
 * 项目状态枚举，定义协作中和已结束两种领域状态。
 */
public enum ProjectStatus {

    /** 协作中，允许成员继续执行写操作。 */
    ACTIVE("active", "协作中"),

    /** 已结束，普通写操作被锁定。 */
    ENDED("ended", "已结束");

    private final String value;
    private final String displayName;

    ProjectStatus(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    /**
     * 获取用于持久化和接口传输的状态值。
     *
     * @return 状态值字符串。
     */
    public String getValue() {
        return value;
    }

    /**
     * 获取用于页面展示的中文名称。
     *
     * @return 中文展示名称。
     */
    public String getDisplayName() {
        return displayName;
    }
}
