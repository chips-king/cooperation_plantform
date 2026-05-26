package com.cooperation.application.packageartifact;

import com.cooperation.domain.packageartifact.PackageFormat;
import java.time.Instant;
import java.util.Objects;

/**
 * 应用层最终压缩包快照模型。
 *
 * @param id 压缩包标识
 * @param projectId 项目标识
 * @param fileName 压缩包文件名
 * @param format 压缩格式
 * @param storageKey 内部存储键
 * @param size 文件大小，单位字节
 * @param createdBy 创建人标识
 * @param createdAt 创建时间
 */
public record PackageArtifact(
        String id,
        String projectId,
        String fileName,
        PackageFormat format,
        String storageKey,
        long size,
        String createdBy,
        Instant createdAt
) {

    /**
     * 创建最终压缩包记录。
     *
     * @param id 压缩包标识
     * @param projectId 项目标识
     * @param fileName 压缩包文件名
     * @param format 压缩格式
     * @param storageKey 内部存储键
     * @param size 文件大小
     * @param createdBy 创建人标识
     * @param createdAt 创建时间
     * @return 最终压缩包记录
     */
    public static PackageArtifact create(
            String id,
            String projectId,
            String fileName,
            PackageFormat format,
            String storageKey,
            long size,
            String createdBy,
            Instant createdAt
    ) {
        return new PackageArtifact(id, projectId, fileName, format, storageKey, size, createdBy, createdAt);
    }

    /**
     * 校验最终压缩包记录字段。
     */
    public PackageArtifact {
        id = requireText(id, "压缩包标识不能为空");
        projectId = requireText(projectId, "项目标识不能为空");
        fileName = requireText(fileName, "压缩包文件名不能为空");
        Objects.requireNonNull(format, "压缩格式不能为空");
        storageKey = requireText(storageKey, "压缩包存储键不能为空");
        if (size < 0) {
            throw new IllegalArgumentException("压缩包大小不能为负数");
        }
        createdBy = requireText(createdBy, "创建人不能为空");
        Objects.requireNonNull(createdAt, "创建时间不能为空");
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
