package com.cooperation.domain.check;

/**
 * 打包前检查目标。
 *
 * @param path 项目内相对路径
 * @param directory 是否为目录
 * @param empty 目录是否为空，文件固定为 false
 * @param size 文件大小，目录固定为 0
 */
public record CheckTarget(
        String path,
        boolean directory,
        boolean empty,
        long size
) {

    /**
     * 创建目录检查目标。
     *
     * @param path 项目内目录路径
     * @param empty 目录是否为空
     * @return 目录检查目标
     */
    public static CheckTarget directory(String path, boolean empty) {
        return new CheckTarget(path, true, empty, 0L);
    }

    /**
     * 创建文件检查目标。
     *
     * @param path 项目内文件路径
     * @param size 文件大小，单位字节
     * @return 文件检查目标
     */
    public static CheckTarget file(String path, long size) {
        return new CheckTarget(path, false, false, size);
    }

    /**
     * 校验检查目标路径和文件大小。
     */
    public CheckTarget {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("检查目标路径不能为空");
        }
        if (size < 0) {
            throw new IllegalArgumentException("文件大小不能为负数");
        }
        path = path.trim();
    }
}
