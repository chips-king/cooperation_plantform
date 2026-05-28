package com.cooperation.application.file;

/**
 * 清空回收站命令，指定项目和操作人。
 *
 * @param projectId 项目标识。
 * @param actorId 操作人用户标识。
 */
public record EmptyTrashCommand(String projectId, String actorId) {
}
