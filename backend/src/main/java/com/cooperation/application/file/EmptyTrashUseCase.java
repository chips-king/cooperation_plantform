package com.cooperation.application.file;

import com.cooperation.application.log.OperationLogWriter;
import com.cooperation.application.notification.NotificationPublisher;
import com.cooperation.application.permission.PermissionChecker;
import com.cooperation.domain.file.FileAsset;
import com.cooperation.domain.file.FileAssetRepository;
import com.cooperation.domain.file.FileAssetStatus;
import com.cooperation.domain.log.OperationAction;
import com.cooperation.domain.log.OperationLog;
import com.cooperation.domain.permission.PermissionCode;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 清空回收站用例，负责永久删除项目中所有已回收的文件。
 */
public final class EmptyTrashUseCase {

    private final FileAssetRepository files;
    private final PermissionChecker permissionChecker;
    private final OperationLogWriter operationLogs;
    private final NotificationPublisher notifications;

    public EmptyTrashUseCase(
            FileAssetRepository files,
            PermissionChecker permissionChecker,
            OperationLogWriter operationLogs,
            NotificationPublisher notifications
    ) {
        this.files = Objects.requireNonNull(files, "文件仓储不能为空");
        this.permissionChecker = Objects.requireNonNull(permissionChecker, "权限检查端口不能为空");
        this.operationLogs = Objects.requireNonNull(operationLogs, "操作记录端口不能为空");
        this.notifications = Objects.requireNonNull(notifications, "通知端口不能为空");
    }

    /**
     * 清空项目回收站。
     *
     * @param command 清空回收站命令。
     * @return 删除的文件数量。
     */
    public int empty(EmptyTrashCommand command) {
        Objects.requireNonNull(command, "清空命令不能为空");

        List<FileAsset> trashedFiles = files.findTrashedByProjectId(command.projectId());
        if (trashedFiles.isEmpty()) {
            return 0;
        }

        // 对回收站中第一个文件检查删除权限
        FileAsset sampleFile = trashedFiles.get(0);
        if (!permissionChecker.hasDirectoryPermission(
                command.actorId(),
                command.projectId(),
                sampleFile.directoryId(),
                PermissionCode.FILE_DELETE)) {
            throw new IllegalStateException("没有清空回收站权限");
        }

        int count = files.deleteByProjectIdAndStatus(command.projectId(), FileAssetStatus.TRASHED);

        operationLogs.write(OperationLog.record(
                command.projectId(),
                command.actorId(),
                OperationAction.FILE_DELETE,
                "project",
                command.projectId(),
                "清空回收站，共删除" + count + "个文件",
                Map.of("deletedCount", String.valueOf(count)),
                Instant.now()
        ));

        notifications.publishFileChanged(command.projectId(), null, command.projectId(), OperationAction.FILE_DELETE);

        return count;
    }
}
