package com.cooperation.web.project;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * 项目 Web API 数据传输对象集合。
 */
public final class ProjectDto {

    private ProjectDto() {
    }

    /**
     * 创建项目请求。
     *
     * @param name 项目名称。
     */
    public record CreateProjectRequest(@NotBlank(message = "项目名称不能为空") String name) {
    }

    /**
     * 创建项目响应。
     *
     * @param projectId 项目标识。
     * @param status 项目状态。
     * @param updatedAt 更新时间。
     */
    public record CreateProjectResponse(Long projectId, String status, Instant updatedAt) {
    }

    /**
     * 项目详情响应。
     *
     * @param id 项目标识。
     * @param groupId 所属小组标识。
     * @param name 项目名称。
     * @param ownerId 项目负责人用户标识。
     * @param status 项目状态。
     * @param endedAt 最近一次结束时间。
     * @param reopenedAt 最近一次重新打开时间。
     */
    public record ProjectDetailResponse(
            Long id,
            Long groupId,
            String name,
            Long ownerId,
            String status,
            LocalDateTime endedAt,
            LocalDateTime reopenedAt
    ) {
    }
}
