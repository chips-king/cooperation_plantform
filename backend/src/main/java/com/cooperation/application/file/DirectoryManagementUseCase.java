package com.cooperation.application.file;

import com.cooperation.web.file.FileDto.DirectoryTreeResponse.DirectoryNodeResponse;

/**
 * 目录管理用例，负责显式创建分工目录与删除空目录。
 */
public interface DirectoryManagementUseCase {

    /**
     * 创建子目录。
     *
     * @param command 创建目录命令
     * @return 新建目录节点
     */
    DirectoryNodeResponse create(CreateCommand command);

    /**
     * 删除空目录。
     *
     * @param command 删除目录命令
     * @return 删除后的父目录定位信息
     */
    DeleteResult deleteEmpty(DeleteCommand command);

    /**
     * 创建目录命令。
     *
     * @param projectId 项目标识
     * @param parentDirectoryId 父目录标识
     * @param name 目录名称
     * @param actorId 操作人标识
     */
    record CreateCommand(String projectId, String parentDirectoryId, String name, String actorId) {
    }

    /**
     * 删除目录命令。
     *
     * @param projectId 项目标识
     * @param directoryId 待删除目录标识
     * @param actorId 操作人标识
     */
    record DeleteCommand(String projectId, String directoryId, String actorId) {
    }

    /**
     * 删除目录响应。
     *
     * @param parentDirectoryId 被删除目录的父目录标识
     */
    record DeleteResult(String parentDirectoryId) {
    }
}
