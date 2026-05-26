package com.cooperation.application.packageartifact;

/**
 * 压缩服务输入条目。
 *
 * @param path 压缩包内相对路径
 * @param storageKey 内部存储键
 * @param size 文件大小，单位字节
 */
public record PackageArchiveEntry(String path, String storageKey, long size) {

    /**
     * 从活动打包源转换为压缩条目。
     *
     * @param source 打包源条目
     * @return 压缩服务输入条目
     */
    public static PackageArchiveEntry fromSource(PackageSourceEntry source) {
        return new PackageArchiveEntry(source.path(), source.storageKey(), source.size());
    }
}
