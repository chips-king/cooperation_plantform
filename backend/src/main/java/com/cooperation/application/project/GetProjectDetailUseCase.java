package com.cooperation.application.project;

import com.cooperation.web.project.ProjectDto;

/**
 * 查询项目详情应用用例契约，供 Web 层读取项目基础信息。
 */
public interface GetProjectDetailUseCase {

    /**
     * 查询当前用户可见的项目详情。
     *
     * @param query 项目详情查询参数。
     * @return 项目详情响应数据。
     */
    ProjectDto.ProjectDetailResponse get(Query query);

    /**
     * 项目详情查询参数。
     *
     * @param actorId 当前操作用户标识。
     * @param projectId 项目标识。
     */
    record Query(Long actorId, Long projectId) {
    }
}
