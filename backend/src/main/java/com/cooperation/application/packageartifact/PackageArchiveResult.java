package com.cooperation.application.packageartifact;

/**
 * 压缩服务结果。
 *
 * @param storageKey 最终压缩包内部存储键
 * @param size 最终压缩包大小，单位字节
 */
public record PackageArchiveResult(String storageKey, long size) {

    /**
     * 校验压缩结果字段。
     */
    public PackageArchiveResult {
        if (storageKey == null || storageKey.isBlank()) {
            throw new IllegalArgumentException("压缩包存储键不能为空");
        }
        storageKey = storageKey.trim();
        if (size < 0) {
            throw new IllegalArgumentException("压缩包大小不能为负数");
        }
    }
}
