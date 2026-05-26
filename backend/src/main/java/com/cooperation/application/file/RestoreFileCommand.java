package com.cooperation.application.file;

/**
 * 恢复文件命令，支持原目录缺失时指定新的恢复目录。
 *
 * @param projectId 项目标识。
 * @param fileId 文件标识。
 * @param restoredBy 恢复人用户标识。
 * @param restoreDirectoryId 用户选择的新恢复目录标识，未选择时为空。
 */
public record RestoreFileCommand(String projectId, String fileId, String restoredBy, String restoreDirectoryId) {
}
