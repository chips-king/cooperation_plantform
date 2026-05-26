package com.cooperation.application.file;

/**
 * 删除文件命令，指定项目、文件和操作人。
 *
 * @param projectId 项目标识。
 * @param fileId 文件标识。
 * @param deletedBy 删除人用户标识。
 */
public record DeleteFileCommand(String projectId, String fileId, String deletedBy) {
}
