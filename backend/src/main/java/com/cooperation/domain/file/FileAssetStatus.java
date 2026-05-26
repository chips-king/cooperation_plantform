package com.cooperation.domain.file;

/**
 * 文件资产状态枚举，表达普通文件、回收站文件和被覆盖历史文件。
 */
public enum FileAssetStatus {

    /**
     * 当前可见文件。
     */
    ACTIVE("active", "正常"),

    /**
     * 已移入回收站的文件。
     */
    TRASHED("trashed", "回收站"),

    /**
     * 被覆盖或被新版本替代的历史文件。
     */
    SUPERSEDED("superseded", "被覆盖");

    private final String value;
    private final String displayName;

    FileAssetStatus(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    /**
     * 获取持久化枚举值。
     *
     * @return 小写状态值。
     */
    public String value() {
        return value;
    }

    /**
     * 获取中文展示名。
     *
     * @return 状态中文说明。
     */
    public String displayName() {
        return displayName;
    }
}
