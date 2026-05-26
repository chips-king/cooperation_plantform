package com.cooperation.application.file;

import com.cooperation.application.directory.DirectoryLookupPort;
import com.cooperation.application.log.OperationLogWriter;
import com.cooperation.application.notification.NotificationPublisher;
import com.cooperation.application.permission.PermissionChecker;
import com.cooperation.domain.file.FileAsset;
import com.cooperation.domain.file.FileAssetRepository;
import com.cooperation.domain.log.OperationAction;
import com.cooperation.domain.log.OperationLog;
import com.cooperation.domain.permission.PermissionCode;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * 文件恢复用例，负责处理回收站文件恢复和原目录缺失场景。
 */
public final class RestoreFileUseCase {

    private final FileAssetRepository files;
    private final DirectoryLookupPort directories;
    private final PermissionChecker permissionChecker;
    private final OperationLogWriter operationLogs;
    private final NotificationPublisher notifications;

    /**
     * 创建文件恢复用例。
     *
     * @param files 文件资产仓储。
     * @param directories 目录查询端口。
     * @param permissionChecker 目录权限检查端口。
     * @param operationLogs 操作记录写入端口。
     * @param notifications 通知发布端口。
     */
    public RestoreFileUseCase(
            FileAssetRepository files,
            DirectoryLookupPort directories,
            PermissionChecker permissionChecker,
            OperationLogWriter operationLogs,
            NotificationPublisher notifications
    ) {
        this.files = Objects.requireNonNull(files, "文件仓储不能为空");
        this.directories = Objects.requireNonNull(directories, "目录查询端口不能为空");
        this.permissionChecker = Objects.requireNonNull(permissionChecker, "权限检查端口不能为空");
        this.operationLogs = Objects.requireNonNull(operationLogs, "操作记录端口不能为空");
        this.notifications = Objects.requireNonNull(notifications, "通知端口不能为空");
    }

    /**
     * 从回收站恢复文件。
     *
     * @param command 恢复文件命令。
     * @return 恢复文件结果。
     */
    public RestoreFileResult restore(RestoreFileCommand command) {
        Objects.requireNonNull(command, "恢复命令不能为空");
        FileAsset file = files.findById(command.fileId()).orElseThrow(() -> new IllegalStateException("文件不存在"));
        String targetDirectoryId = resolveTargetDirectory(command, file);
        if (targetDirectoryId == null) {
            return RestoreFileResult.requireDirectorySelection();
        }
        if (!permissionChecker.hasDirectoryPermission(command.restoredBy(), command.projectId(), targetDirectoryId, PermissionCode.FILE_RESTORE)) {
            throw new IllegalStateException("没有恢复权限");
        }

        file.restoreToDirectory(targetDirectoryId);
        FileAsset saved = files.save(file);
        operationLogs.write(OperationLog.record(
                command.projectId(),
                command.restoredBy(),
                OperationAction.FILE_RESTORE,
                "file",
                saved.id(),
                "恢复文件：" + saved.name().value(),
                Map.of("restoreDirectoryId", targetDirectoryId),
                Instant.now()
        ));
        notifications.publishFileChanged(command.projectId(), targetDirectoryId, saved.id(), OperationAction.FILE_RESTORE);
        return RestoreFileResult.restored(saved);
    }

    private String resolveTargetDirectory(RestoreFileCommand command, FileAsset file) {
        if (command.restoreDirectoryId() != null && directories.existsByProjectIdAndDirectoryId(command.projectId(), command.restoreDirectoryId())) {
            return command.restoreDirectoryId();
        }
        if (directories.existsByProjectIdAndDirectoryId(command.projectId(), file.directoryId())) {
            return file.directoryId();
        }
        return null;
    }
}
