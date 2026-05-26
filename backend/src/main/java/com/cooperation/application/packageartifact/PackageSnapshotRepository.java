package com.cooperation.application.packageartifact;

import java.time.Instant;
import java.util.List;

/**
 * 最终打包快照仓储，用于读取参与压缩的项目文件快照。
 */
public interface PackageSnapshotRepository {

    /**
     * 查询项目当前打包快照条目。
     *
     * @param projectId 项目标识
     * @return 打包源条目列表
     */
    List<PackageSourceEntry> findSnapshotEntries(String projectId);

    /**
     * 查询项目打包快照创建时间。
     *
     * @param projectId 项目标识
     * @return 快照创建时间
     */
    Instant snapshotCreatedAt(String projectId);
}
