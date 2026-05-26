package com.cooperation.domain.file;

import java.util.List;
import java.util.Optional;

/**
 * 文件资产仓储抽象，只定义领域所需查询与保存能力。
 */
public interface FileAssetRepository {

    /**
     * 保存文件资产。
     *
     * @param fileAsset 待保存的文件资产。
     * @return 保存后的文件资产。
     */
    FileAsset save(FileAsset fileAsset);

    /**
     * 按文件标识查询文件资产。
     *
     * @param id 文件唯一标识。
     * @return 匹配的文件资产，不存在时为空。
     */
    Optional<FileAsset> findById(String id);

    /**
     * 查询同目录下的当前同名文件。
     *
     * @param directoryId 目录标识。
     * @param name 文件展示名。
     * @return 匹配的当前文件，不存在时为空。
     */
    Optional<FileAsset> findActiveByDirectoryIdAndName(String directoryId, FileName name);

    /**
     * 查询项目下当前可见文件。
     *
     * @param projectId 项目标识。
     * @return 项目文件树中的当前文件集合。
     */
    List<FileAsset> findActiveByProjectId(String projectId);

    /**
     * 查询项目回收站文件。
     *
     * @param projectId 项目标识。
     * @return 项目回收站文件集合。
     */
    List<FileAsset> findTrashedByProjectId(String projectId);
}
