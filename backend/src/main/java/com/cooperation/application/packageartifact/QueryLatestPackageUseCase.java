package com.cooperation.application.packageartifact;

import java.time.Instant;
import java.util.Objects;

/**
 * 查询项目最近压缩包的应用层端口。
 */
public interface QueryLatestPackageUseCase {

    /**
     * 查询指定项目对当前用户可见的最近压缩包。
     *
     * @param query 最近压缩包查询参数
     * @return 最近压缩包摘要
     */
    Result query(Query query);

    /**
     * 最近压缩包查询参数。
     *
     * @param projectId 项目标识
     * @param actorId 当前用户标识
     */
    record Query(String projectId, String actorId) {

        /**
         * 校验最近压缩包查询参数。
         */
        public Query {
            projectId = requireText(projectId, "项目标识不能为空");
            actorId = requireText(actorId, "当前用户不能为空");
        }
    }

    /**
     * 最近压缩包摘要。
     *
     * @param packageId 压缩包标识
     * @param filename 压缩包展示文件名
     * @param format 压缩包格式
     * @param snapshotCreatedAt 快照创建时间
     * @param size 压缩包大小，单位字节
     */
    record Result(String packageId, String filename, String format, Instant snapshotCreatedAt, long size) {

        /**
         * 校验最近压缩包摘要。
         */
        public Result {
            packageId = requireText(packageId, "压缩包标识不能为空");
            filename = requireText(filename, "压缩包文件名不能为空");
            format = requireText(format, "压缩包格式不能为空");
            Objects.requireNonNull(snapshotCreatedAt, "快照创建时间不能为空");
            if (size < 0) {
                throw new IllegalArgumentException("压缩包大小不能为负数");
            }
        }
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
