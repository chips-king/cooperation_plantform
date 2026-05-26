package com.cooperation.application.file;

import com.cooperation.application.log.OperationLogWriter;
import com.cooperation.application.notification.NotificationPublisher;
import com.cooperation.application.permission.PermissionChecker;
import com.cooperation.domain.file.FileAsset;
import com.cooperation.domain.file.FileAssetRepository;
import com.cooperation.domain.file.FileName;
import com.cooperation.domain.file.FileAssetStatus;
import com.cooperation.domain.log.OperationAction;
import com.cooperation.domain.log.OperationLog;
import com.cooperation.domain.permission.PermissionCode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 文件删除应用用例测试，表达删除进入回收站、记录和通知契约。
 */
class DeleteFileUseCaseTest {

    private final FakeFileAssetRepository files = new FakeFileAssetRepository();
    private final FakePermissionChecker permissionChecker = new FakePermissionChecker();
    private final FakeOperationLogWriter operationLogs = new FakeOperationLogWriter();
    private final FakeNotificationPublisher notifications = new FakeNotificationPublisher();
    private final DeleteFileUseCase useCase = new DeleteFileUseCase(files, permissionChecker, operationLogs, notifications);

    /**
     * 验证有目录权限的成员删除文件后，文件进入回收站并写入删除记录。
     */
    @Test
    void shouldMoveFileToTrashAndWriteOperationLog() {
        FileAsset file = uploadedFile("file-1", "report.docx", "directory-1");
        files.save(file);
        permissionChecker.allow("member-1", "project-1", "directory-1", PermissionCode.FILE_DELETE);

        DeleteFileResult result = useCase.delete(new DeleteFileCommand("project-1", "file-1", "member-1"));

        assertThat(result.file().status()).isEqualTo(FileAssetStatus.TRASHED);
        assertThat(result.file().deletedBy()).isEqualTo("member-1");
        assertThat(files.findById("file-1")).contains(result.file());
        assertThat(operationLogs.actions()).containsExactly(OperationAction.FILE_DELETE);
        assertThat(operationLogs.last().getTargetId()).isEqualTo("file-1");
        assertThat(notifications.events).containsExactly("file.deleted:directory-1");
    }

    /**
     * 验证无删除权限时不会改变文件状态，也不会写入记录。
     */
    @Test
    void shouldRejectDeleteWhenUserHasNoDirectoryPermission() {
        FileAsset file = uploadedFile("file-1", "report.docx", "directory-1");
        files.save(file);

        assertThatThrownBy(() -> useCase.delete(new DeleteFileCommand("project-1", "file-1", "readonly-1")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("没有删除权限");

        assertThat(file.status()).isEqualTo(FileAssetStatus.ACTIVE);
        assertThat(operationLogs.logs).isEmpty();
        assertThat(notifications.events).isEmpty();
    }

    /**
     * 创建文件测试样本，删除测试只关注状态和审计行为。
     */
    private FileAsset uploadedFile(String id, String name, String directoryId) {
        return FileAsset.uploaded(
                id,
                "project-1",
                directoryId,
                FileName.of(name),
                1024L,
                "application/octet-stream",
                "project-files/" + id,
                "member-1",
                "version-" + id,
                1
        );
    }

    /**
     * 文件仓储假实现，避免依赖数据库。
     */
    private static final class FakeFileAssetRepository implements FileAssetRepository {
        private final List<FileAsset> saved = new ArrayList<>();

        @Override
        public FileAsset save(FileAsset fileAsset) {
            saved.removeIf(existing -> existing.id().equals(fileAsset.id()));
            saved.add(fileAsset);
            return fileAsset;
        }

        @Override
        public Optional<FileAsset> findById(String id) {
            return saved.stream().filter(file -> file.id().equals(id)).findFirst();
        }

        @Override
        public Optional<FileAsset> findActiveByDirectoryIdAndName(String directoryId, FileName name) {
            return saved.stream().filter(FileAsset::isActive).filter(file -> file.directoryId().equals(directoryId)).filter(file -> file.name().value().equals(name.value())).findFirst();
        }

        @Override
        public List<FileAsset> findActiveByProjectId(String projectId) {
            return saved.stream().filter(FileAsset::isActive).filter(file -> file.projectId().equals(projectId)).toList();
        }

        @Override
        public List<FileAsset> findTrashedByProjectId(String projectId) {
            return saved.stream().filter(file -> file.projectId().equals(projectId)).filter(file -> file.status() == FileAssetStatus.TRASHED).toList();
        }
    }

    /**
     * 权限检查假实现，按目录和权限点授权。
     */
    private static final class FakePermissionChecker implements PermissionChecker {
        private final List<String> allowed = new ArrayList<>();

        void allow(String userId, String projectId, String directoryId, PermissionCode permissionCode) {
            allowed.add(userId + "|" + projectId + "|" + directoryId + "|" + permissionCode.code());
        }

        @Override
        public boolean hasDirectoryPermission(String userId, String projectId, String directoryId, PermissionCode permissionCode) {
            return allowed.contains(userId + "|" + projectId + "|" + directoryId + "|" + permissionCode.code());
        }
    }

    /**
     * 操作记录写入假实现。
     */
    private static final class FakeOperationLogWriter implements OperationLogWriter {
        private final List<OperationLog> logs = new ArrayList<>();

        @Override
        public void write(OperationLog operationLog) {
            logs.add(operationLog);
        }

        List<OperationAction> actions() {
            return logs.stream().map(OperationLog::getAction).toList();
        }

        OperationLog last() {
            return logs.get(logs.size() - 1);
        }
    }

    /**
     * 通知发布假实现。
     */
    private static final class FakeNotificationPublisher implements NotificationPublisher {
        private final List<String> events = new ArrayList<>();

        @Override
        public void publishFileChanged(String projectId, String directoryId, String fileId, OperationAction action) {
            events.add("file.deleted:" + directoryId);
        }
    }
}
