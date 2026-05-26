package com.cooperation.web.progress;

import java.time.Instant;
import java.util.List;

/**
 * 项目进度 Web DTO 集合，定义进度查询和目录状态更新接口结构。
 */
public final class ProgressDto {

    private ProgressDto() {
    }

    /**
     * 项目进度汇总响应。
     *
     * @param projectId 项目标识。
     * @param totalDirectoryCount 目录总数。
     * @param completedDirectoryCount 已完成目录数。
     * @param directories 目录进度列表。
     */
    public record ProjectProgressResponse(
            String projectId,
            int totalDirectoryCount,
            int completedDirectoryCount,
            List<DirectoryProgressResponse> directories
    ) {
    }

    /**
     * 单个目录进度响应。
     *
     * @param directoryId 目录标识。
     * @param name 目录名称。
     * @param status 目录状态值。
     * @param statusDisplayName 目录状态中文展示名。
     * @param updatedAt 最近更新时间。
     */
    public record DirectoryProgressResponse(
            String directoryId,
            String name,
            String status,
            String statusDisplayName,
            Instant updatedAt
    ) {
    }

    /**
     * 更新目录状态请求。
     *
     * @param projectId 项目标识。
     * @param status 目标目录状态值。
     */
    public record UpdateDirectoryStatusRequest(String projectId, String status) {
    }

    /**
     * 目录状态更新响应。
     *
     * @param directoryId 目录标识。
     * @param name 目录名称。
     * @param status 目录状态值。
     * @param statusDisplayName 目录状态中文展示名。
     */
    public record DirectoryStatusResponse(
            String directoryId,
            String name,
            String status,
            String statusDisplayName
    ) {
    }
}
