package com.cooperation.application.file;

import com.cooperation.web.file.FileDto.DirectoryTreeResponse;

/**
 * 查询项目目录树用例端口，供 Web 层获取目录与文件展示结构。
 */
public interface ListDirectoryTreeUseCase {

    /**
     * 查询指定项目的目录树。
     *
     * @param projectId 项目标识。
     * @return 项目目录树响应数据。
     */
    DirectoryTreeResponse getTree(String projectId);
}
