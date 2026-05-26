package com.cooperation.application.directory;

import com.cooperation.web.progress.ProgressDto.ProjectProgressResponse;

/**
 * 查询项目目录进度用例端口。
 */
public interface ListProjectProgressUseCase {

    /**
     * 查询指定项目的目录进度汇总。
     *
     * @param projectId 项目标识。
     * @return 项目进度响应数据。
     */
    ProjectProgressResponse getProgress(String projectId);
}
