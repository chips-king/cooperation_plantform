package com.cooperation.application.packageartifact;

import com.cooperation.domain.file.FileAsset;
import java.util.Optional;

/**
 * 清理目标仓储，用于按项目内路径定位可清理文件。
 */
public interface CleanupTargetRepository {

    /**
     * 查询项目内指定路径的活动文件。
     *
     * @param projectId 项目标识
     * @param path 项目内相对路径
     * @return 匹配的活动文件
     */
    Optional<FileAsset> findActiveFileByProjectIdAndPath(String projectId, String path);

    /**
     * 保存清理后的文件状态。
     *
     * @param fileAsset 文件资产
     * @return 保存后的文件资产
     */
    FileAsset save(FileAsset fileAsset);
}
