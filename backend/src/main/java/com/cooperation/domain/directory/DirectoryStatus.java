package com.cooperation.domain.directory;

/**
 * 目录任务状态枚举，定义未开始、进行中和已完成三态进度。
 */
public enum DirectoryStatus {

    /** 未开始，表示目录任务尚未进入协作处理。 */
    NOT_STARTED("not_started", "未开始"),

    /** 进行中，表示目录任务正在协作处理。 */
    IN_PROGRESS("in_progress", "进行中"),

    /** 已完成，表示目录任务已完成交付。 */
    COMPLETED("completed", "已完成");

    private final String value;
    private final String displayName;

    DirectoryStatus(String value, String displayName) {
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
