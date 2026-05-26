package com.cooperation.application.directory;

import com.cooperation.domain.directory.DirectoryNode;

import java.util.Optional;

/**
 * 目录仓储端口，提供目录状态用例所需的查询与保存能力。
 */
public interface DirectoryRepository {

    /**
     * 保存目录实体。
     *
     * @param directoryId 目录标识。
     * @param directory 目录实体。
     * @return 保存后的目录实体。
     */
    DirectoryNode save(String directoryId, DirectoryNode directory);

    /**
     * 查询项目下指定目录。
     *
     * @param projectId 项目标识。
     * @param directoryId 目录标识。
     * @return 匹配目录，不存在时为空。
     */
    Optional<DirectoryNode> findByProjectIdAndDirectoryId(String projectId, String directoryId);
}
