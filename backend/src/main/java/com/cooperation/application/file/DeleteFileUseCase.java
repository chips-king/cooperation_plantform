package com.cooperation.application.file;

import com.cooperation.application.log.OperationLogWriter;
import com.cooperation.application.notification.NotificationPublisher;
import com.cooperation.application.permission.PermissionChecker;
import com.cooperation.domain.file.FileAsset;
import com.cooperation.domain.file.FileAssetRepository;
import com.cooperation.domain.log.OperationAction;
import com.cooperation.domain.log.OperationLog;
import com.cooperation.domain.permission.PermissionCode;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

/**
 * 文件删除用例，负责将文件移入回收站并记录审计与通知。
 */
public final class DeleteFileUseCase {

    private final FileAssetRepository files;
    private final PermissionChecker permissionChecker;
    private final OperationLogWriter operationLogs;
    private final NotificationPublisher notifications;

    /**
     * 创建文件删除用例。
     *
     * @param files 文件资产仓储。
     * @param permissionChecker 目录权限检查端口。
     * @param operationLogs 操作记录写入端口。
     * @param notifications 通知发布端口。
     */
    public DeleteFileUseCase(
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
     * 删除文件到回收站。
     *
     * @param command 删除文件命令。
     * @return 删除文件结果。
     */
    public DeleteFileResult delete(DeleteFileCommand command) {
        Objects.requireNonNull(command, "删除命令不能为空");
        FileAsset file = files.findById(command.fileId()).orElseThrow(() -> new IllegalStateException("文件不存在"));
        if (!permissionChecker.hasDirectoryPermission(command.deletedBy(), command.projectId(), file.directoryId(), PermissionCode.FILE_DELETE)) {
            throw new IllegalStateException("没有删除权限");
        }

        file.moveToTrash(command.deletedBy(), LocalDateTime.now());
        FileAsset saved = files.save(file);
        operationLogs.write(OperationLog.record(
                command.projectId(),
                command.deletedBy(),
                OperationAction.FILE_DELETE,
                "file",
                saved.id(),
                "删除文件：" + saved.name().value(),
                Map.of("directoryId", saved.directoryId()),
                Instant.now()
        ));
        notifications.publishFileChanged(command.projectId(), saved.directoryId(), saved.id(), OperationAction.FILE_DELETE);
        return new DeleteFileResult(saved);
    }
}
