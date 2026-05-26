package com.cooperation.application.directory;

import com.cooperation.domain.directory.DirectoryStatus;

/**
 * 更新目录状态命令。
 *
 * @param projectId 项目标识。
 * @param directoryId 目录标识。
 * @param operatorId 操作人用户标识。
 * @param nextStatus 目标目录状态。
 */
public record UpdateDirectoryStatusCommand(
        String projectId,
        String directoryId,
        String operatorId,
        DirectoryStatus nextStatus
) {
}
