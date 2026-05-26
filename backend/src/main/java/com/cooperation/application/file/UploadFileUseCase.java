package com.cooperation.application.file;

import com.cooperation.application.log.OperationLogWriter;
import com.cooperation.application.notification.NotificationPublisher;
import com.cooperation.application.permission.PermissionChecker;
import com.cooperation.domain.file.DuplicateFilePolicy;
import com.cooperation.domain.file.DuplicateFileResult;
import com.cooperation.domain.file.FileAsset;
import com.cooperation.domain.file.FileAssetRepository;
import com.cooperation.domain.file.FileName;
import com.cooperation.domain.log.OperationAction;
import com.cooperation.domain.log.OperationLog;
import com.cooperation.domain.permission.PermissionCode;

import java.nio.file.Paths;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 文件上传用例，负责编排权限校验、内容存储、同名策略、操作记录与通知。
 */
public final class UploadFileUseCase {

    private final FileAssetRepository files;
    private final FileStoragePort storage;
    private final PermissionChecker permissionChecker;
    private final OperationLogWriter operationLogs;
    private final NotificationPublisher notifications;

    /**
     * 创建文件上传用例。
     *
     * @param files 文件资产仓储。
     * @param storage 文件内容存储端口。
     * @param permissionChecker 目录权限检查端口。
     * @param operationLogs 操作记录写入端口。
     * @param notifications 通知发布端口。
     */
    public UploadFileUseCase(
            FileAssetRepository files,
            FileStoragePort storage,
            PermissionChecker permissionChecker,
            OperationLogWriter operationLogs,
            NotificationPublisher notifications
    ) {
        this.files = Objects.requireNonNull(files, "文件仓储不能为空");
        this.storage = Objects.requireNonNull(storage, "文件存储端口不能为空");
        this.permissionChecker = Objects.requireNonNull(permissionChecker, "权限检查端口不能为空");
        this.operationLogs = Objects.requireNonNull(operationLogs, "操作记录端口不能为空");
        this.notifications = Objects.requireNonNull(notifications, "通知端口不能为空");
    }

    /**
     * 上传文件并返回保存后的文件资产。
     *
     * @param command 上传文件命令。
     * @return 上传文件结果。
     */
    public UploadFileResult upload(UploadFileCommand command) {
        Objects.requireNonNull(command, "上传命令不能为空");
        if (!permissionChecker.hasDirectoryPermission(command.uploadedBy(), command.projectId(), command.directoryId(), PermissionCode.FILE_UPLOAD)) {
            throw new IllegalStateException("没有上传权限");
        }

        DuplicateFilePolicy duplicatePolicy = command.duplicatePolicy() == null
                ? DuplicateFilePolicy.NEW_VERSION
                : command.duplicatePolicy();
        FileName requestedName = FileName.of(displayName(command.originalFilename()));
        FileName actualName = duplicatePolicy == DuplicateFilePolicy.RENAME && command.renamedFilename() != null
                ? FileName.of(command.renamedFilename())
                : requestedName;
        String storageKey = storage.save(command.projectId(), command.directoryId(), actualName.value(), command.content());
        FileAsset newFile = FileAsset.uploaded(
                UUID.randomUUID().toString(),
                command.projectId(),
                command.directoryId(),
                actualName,
                command.size(),
                command.mimeType(),
                storageKey,
                command.uploadedBy(),
                UUID.randomUUID().toString(),
                1
        );

        FileAsset savedFile = files.findActiveByDirectoryIdAndName(command.directoryId(), requestedName)
                .map(oldFile -> saveDuplicate(command, duplicatePolicy, oldFile, newFile, actualName))
                .orElseGet(() -> files.save(newFile));
        writeLog(command, duplicatePolicy, savedFile);
        notifications.publishFileChanged(command.projectId(), command.directoryId(), savedFile.id(), OperationAction.FILE_UPLOAD);
        return new UploadFileResult(savedFile, command.archive());
    }

    private FileAsset saveDuplicate(UploadFileCommand command, DuplicateFilePolicy duplicatePolicy, FileAsset oldFile, FileAsset newFile, FileName actualName) {
        DuplicateFileResult result = duplicatePolicy == DuplicateFilePolicy.RENAME
                ? duplicatePolicy.apply(oldFile, newFile, actualName)
                : duplicatePolicy.apply(oldFile, newFile);
        files.save(oldFile);
        return files.save(result.currentFile());
    }

    private void writeLog(UploadFileCommand command, DuplicateFilePolicy duplicatePolicy, FileAsset file) {
        operationLogs.write(OperationLog.record(
                command.projectId(),
                command.uploadedBy(),
                OperationAction.FILE_UPLOAD,
                "file",
                file.id(),
                "上传文件：" + file.name().value(),
                Map.of(
                        "directoryId", command.directoryId(),
                        "duplicatePolicy", duplicatePolicy.name(),
                        "archive", Boolean.toString(command.archive())
                ),
                Instant.now()
        ));
    }

    private String displayName(String originalFilename) {
        return Paths.get(originalFilename).getFileName().toString();
    }
}
