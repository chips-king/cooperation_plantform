package com.cooperation.application.directory;

import com.cooperation.application.log.OperationLogWriter;
import com.cooperation.application.notification.NotificationPublisher;
import com.cooperation.application.permission.PermissionChecker;
import com.cooperation.domain.directory.DirectoryNode;
import com.cooperation.domain.log.OperationAction;
import com.cooperation.domain.log.OperationLog;
import com.cooperation.domain.permission.PermissionCode;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * 目录状态更新用例，负责权限校验、领域状态变更、操作记录和通知。
 */
public final class UpdateDirectoryStatusUseCase {

    private final DirectoryRepository directories;
    private final PermissionChecker permissionChecker;
    private final OperationLogWriter operationLogs;
    private final NotificationPublisher notifications;

    /**
     * 创建目录状态更新用例。
     *
     * @param directories 目录仓储。
     * @param permissionChecker 目录权限检查端口。
     * @param operationLogs 操作记录写入端口。
     * @param notifications 通知发布端口。
     */
    public UpdateDirectoryStatusUseCase(
            DirectoryRepository directories,
            PermissionChecker permissionChecker,
            OperationLogWriter operationLogs,
            NotificationPublisher notifications
    ) {
        this.directories = Objects.requireNonNull(directories, "目录仓储不能为空");
        this.permissionChecker = Objects.requireNonNull(permissionChecker, "权限检查端口不能为空");
        this.operationLogs = Objects.requireNonNull(operationLogs, "操作记录端口不能为空");
        this.notifications = Objects.requireNonNull(notifications, "通知端口不能为空");
    }

    /**
     * 更新目录任务状态。
     *
     * @param command 更新目录状态命令。
     * @return 更新目录状态结果。
     */
    public UpdateDirectoryStatusResult update(UpdateDirectoryStatusCommand command) {
        Objects.requireNonNull(command, "目录状态更新命令不能为空");
        if (!permissionChecker.hasDirectoryPermission(command.operatorId(), command.projectId(), command.directoryId(), PermissionCode.DIRECTORY_STATUS_UPDATE)) {
            throw new IllegalStateException("没有目录状态更新权限");
        }

        DirectoryNode directory = directories.findByProjectIdAndDirectoryId(command.projectId(), command.directoryId())
                .orElseThrow(() -> new IllegalStateException("目录不存在"));
        directory.changeStatus(command.nextStatus(), numericUserId(command.operatorId()));
        DirectoryNode saved = directories.save(command.directoryId(), directory);
        operationLogs.write(OperationLog.record(
                command.projectId(),
                command.operatorId(),
                OperationAction.DIRECTORY_STATUS_UPDATE,
                "directory",
                command.directoryId(),
                "更新目录状态：" + command.nextStatus().getDisplayName(),
                Map.of("nextStatus", command.nextStatus().getValue()),
                Instant.now()
        ));
        notifications.publishDirectoryStatusChanged(command.projectId(), command.directoryId(), command.nextStatus());
        return new UpdateDirectoryStatusResult(saved);
    }

    private Long numericUserId(String operatorId) {
        String digits = operatorId == null ? "" : operatorId.replaceAll("\\D+", "");
        if (digits.isEmpty()) {
            throw new IllegalArgumentException("操作人标识缺少数字部分");
        }
        return Long.parseLong(digits);
    }
}
