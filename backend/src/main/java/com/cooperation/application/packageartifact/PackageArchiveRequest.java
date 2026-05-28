package com.cooperation.application.packageartifact;

import com.cooperation.domain.packageartifact.PackageFormat;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * 压缩服务请求。
 *
 * @param packageId 压缩包标识，用于生成唯一存储路径
 * @param fileName 最终压缩包文件名
 * @param format 压缩格式
 * @param snapshotCreatedAt 打包快照创建时间
 * @param entries 需要进入压缩包的条目
 */
public record PackageArchiveRequest(
        String packageId,
        String fileName,
        PackageFormat format,
        Instant snapshotCreatedAt,
        List<PackageArchiveEntry> entries
) {

    /**
     * 校验压缩请求字段并复制条目列表。
     */
    public PackageArchiveRequest {
        if (packageId == null || packageId.isBlank()) {
            throw new IllegalArgumentException("压缩包标识不能为空");
        }
        packageId = packageId.trim();
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("压缩包文件名不能为空");
        }
        fileName = fileName.trim();
        Objects.requireNonNull(format, "压缩格式不能为空");
        Objects.requireNonNull(snapshotCreatedAt, "打包快照创建时间不能为空");
        entries = List.copyOf(Objects.requireNonNull(entries, "压缩条目不能为空"));
    }
}
