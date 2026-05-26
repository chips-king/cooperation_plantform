package com.cooperation.application.packageartifact;

/**
 * 项目打包源条目。
 *
 * @param path 压缩包内相对路径
 * @param storageKey 内部存储键
 * @param size 文件大小，单位字节
 * @param kind 条目类型
 */
public record PackageSourceEntry(String path, String storageKey, long size, Kind kind) {

    /**
     * 创建活动文件打包源。
     *
     * @param path 压缩包内相对路径
     * @param storageKey 内部存储键
     * @param size 文件大小，单位字节
     * @return 活动文件源条目
     */
    public static PackageSourceEntry activeFile(String path, String storageKey, long size) {
        return new PackageSourceEntry(path, storageKey, size, Kind.ACTIVE_FILE);
    }

    /**
     * 创建回收站文件打包源。
     *
     * @param path 压缩包内相对路径
     * @param storageKey 内部存储键
     * @param size 文件大小，单位字节
     * @return 回收站文件源条目
     */
    public static PackageSourceEntry trashedFile(String path, String storageKey, long size) {
        return new PackageSourceEntry(path, storageKey, size, Kind.TRASHED_FILE);
    }

    /**
     * 创建旧压缩包打包源。
     *
     * @param path 压缩包内相对路径
     * @param storageKey 内部存储键
     * @param size 文件大小，单位字节
     * @return 旧压缩包源条目
     */
    public static PackageSourceEntry oldPackage(String path, String storageKey, long size) {
        return new PackageSourceEntry(path, storageKey, size, Kind.OLD_PACKAGE);
    }

    /**
     * 判断条目是否应进入最终压缩包。
     *
     * @return 活动文件返回 true
     */
    public boolean includedInPackage() {
        return kind == Kind.ACTIVE_FILE;
    }

    /**
     * 校验打包源条目字段。
     */
    public PackageSourceEntry {
        path = requireText(path, "打包源路径不能为空");
        storageKey = requireText(storageKey, "打包源存储键不能为空");
        if (size < 0) {
            throw new IllegalArgumentException("打包源大小不能为负数");
        }
        if (kind == null) {
            throw new IllegalArgumentException("打包源类型不能为空");
        }
    }

    /**
     * 打包源条目类型。
     */
    public enum Kind {
        /** 当前活动文件，会进入最终压缩包。 */
        ACTIVE_FILE,

        /** 回收站文件，不进入最终压缩包。 */
        TRASHED_FILE,

        /** 历史最终压缩包，不进入最终压缩包。 */
        OLD_PACKAGE
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
