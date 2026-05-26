package com.cooperation.application.packageartifact;

import com.cooperation.domain.check.ProjectFileTree;

/**
 * 打包检查项目快照仓储，用于为检查规则提供项目文件树。
 */
public interface ProjectPackageSnapshotRepository {

    /**
     * 查询项目当前用于打包检查的文件树快照。
     *
     * @param projectId 项目标识
     * @return 项目文件树快照
     */
    ProjectFileTree findCheckTreeByProjectId(String projectId);
}
