package com.cooperation.application.notification;

import com.cooperation.domain.directory.DirectoryStatus;
import com.cooperation.domain.log.OperationAction;

/**
 * 应用层通知发布端口。
 */
public interface NotificationPublisher {

    /**
     * 发布文件变化通知。
     *
     * @param projectId 项目标识
     * @param directoryId 目录标识
     * @param fileId 文件标识
     * @param action 文件操作类型
     */
    default void publishFileChanged(String projectId, String directoryId, String fileId, OperationAction action) {
    }

    /**
     * 发布目录状态变化通知。
     *
     * @param projectId 项目标识
     * @param directoryId 目录标识
     * @param nextStatus 变更后的目录状态
     */
    default void publishDirectoryStatusChanged(String projectId, String directoryId, DirectoryStatus nextStatus) {
    }
}
