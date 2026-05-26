package com.cooperation.application.file;

import com.cooperation.domain.file.FileAsset;

import java.util.Optional;

/**
 * 恢复文件结果，表达是否需要用户重新选择恢复目录。
 *
 * @param requiresDirectorySelection 原目录缺失且未选择目录时为 true。
 * @param file 恢复成功后的文件资产，等待选择目录时为空。
 */
public record RestoreFileResult(boolean requiresDirectorySelection, Optional<FileAsset> file) {

    /**
     * 创建需要重新选择目录的结果。
     *
     * @return 不包含文件资产的恢复结果。
     */
    public static RestoreFileResult requireDirectorySelection() {
        return new RestoreFileResult(true, Optional.empty());
    }

    /**
     * 创建恢复成功结果。
     *
     * @param file 已恢复的文件资产。
     * @return 包含文件资产的恢复结果。
     */
    public static RestoreFileResult restored(FileAsset file) {
        return new RestoreFileResult(false, Optional.of(file));
    }
}
