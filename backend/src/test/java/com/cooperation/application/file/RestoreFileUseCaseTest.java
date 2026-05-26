package com.cooperation.application.file;

import com.cooperation.application.directory.DirectoryLookupPort;
import com.cooperation.application.log.OperationLogWriter;
import com.cooperation.application.notification.NotificationPublisher;
import com.cooperation.application.permission.PermissionChecker;
import com.cooperation.domain.file.FileAsset;
import com.cooperation.domain.file.FileAssetRepository;
import com.cooperation.domain.file.FileAssetStatus;
import com.cooperation.domain.file.FileName;
import com.cooperation.domain.log.OperationAction;
import com.cooperation.domain.log.OperationLog;
import com.cooperation.domain.permission.PermissionCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 文件恢复应用用例测试，表达回收站恢复和原目录缺失处理契约。
 */
class RestoreFileUseCaseTest {

    private final FakeFileAssetRepository files = new FakeFileAssetRepository();
    private final FakeDirectoryLookupPort directories = new FakeDirectoryLookupPort();
    private final FakePermissionChecker permissionChecker = new FakePermissionChecker();
    private final FakeOperationLogWriter operationLogs = new FakeOperationLogWriter();
    private final FakeNotificationPublisher notifications = new FakeNotificationPublisher();
    private final RestoreFileUseCase useCase = new RestoreFileUseCase(files, directories, permissionChecker, operationLogs, notifications);

    /**
     * 验证原目录存在且用户有恢复权限时，文件从回收站恢复并写入记录。
     */
    @Test
    void shouldRestoreFileToOriginalDirectoryAndWriteOperationLog() {
        FileAsset file = trashedFile("file-1", "report.docx", "directory-1");
        files.save(file);
        directories.exists("project-1", "directory-1");
        permissionChecker.allow("member-1", "project-1", "directory-1", PermissionCode.FILE_RESTORE);

        RestoreFileResult result = useCase.restore(new RestoreFileCommand("project-1", "file-1", "member-1", null));

        assertThat(result.requiresDirectorySelection()).isFalse();
        assertThat(result.file()).isPresent();
        assertThat(result.file().orElseThrow().status()).isEqualTo(FileAssetStatus.ACTIVE);
        assertThat(operationLogs.actions()).containsExactly(OperationAction.FILE_RESTORE);
        assertThat(notifications.events).containsExactly("file.restored:directory-1");
    }

    /**
     * 验证原目录不存在且未选择恢复目录时，用例返回需要选择目录而不恢复文件。
     */
    @Test
    void shouldRequireRestoreDirectoryWhenOriginalDirectoryMissing() {
        FileAsset file = trashedFile("file-1", "report.docx", "directory-missing");
        files.save(file);

        RestoreFileResult result = useCase.restore(new RestoreFileCommand("project-1", "file-1", "member-1", null));

        assertThat(result.requiresDirectorySelection()).isTrue();
        assertThat(result.file()).isEmpty();
        assertThat(file.status()).isEqualTo(FileAssetStatus.TRASHED);
        assertThat(operationLogs.logs).isEmpty();
    }

    /**
     * 验证原目录不存在但用户选择有权限目录时，文件恢复到新目录并记录恢复动作。
     */
    @Test
    void shouldRestoreToSelectedDirectoryWhenOriginalDirectoryMissing() {
        FileAsset file = trashedFile("file-1", "report.docx", "directory-missing");
        files.save(file);
        directories.exists("project-1", "directory-2");
        permissionChecker.allow("member-1", "project-1", "directory-2", PermissionCode.FILE_RESTORE);

        RestoreFileResult result = useCase.restore(new RestoreFileCommand("project-1", "file-1", "member-1", "directory-2"));

        assertThat(result.requiresDirectorySelection()).isFalse();
        assertThat(result.file()).isPresent();
        assertThat(result.file().orElseThrow().status()).isEqualTo(FileAssetStatus.ACTIVE);
        assertThat(result.file().orElseThrow().directoryId()).isEqualTo("directory-2");
        assertThat(operationLogs.last().getMetadata()).containsEntry("restoreDirectoryId", "directory-2");
    }

    /**
     * 验证无恢复权限时不会改变回收站文件。
     */
    @Test
    void shouldRejectRestoreWhenUserHasNoPermission() {
        FileAsset file = trashedFile("file-1", "report.docx", "directory-1");
        files.save(file);
        directories.exists("project-1", "directory-1");

        assertThatThrownBy(() -> useCase.restore(new RestoreFileCommand("project-1", "file-1", "readonly-1", null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("没有恢复权限");

        assertThat(file.status()).isEqualTo(FileAssetStatus.TRASHED);
        assertThat(operationLogs.logs).isEmpty();
    }

    /**
     * 创建回收站文件测试样本。
     */
    private FileAsset trashedFile(String id, String name, String directoryId) {
        FileAsset file = FileAsset.uploaded(id, "project-1", directoryId, FileName.of(name), 1024L, "application/octet-stream", "trash/" + id, "member-1", "version-" + id, 1);
        file.moveToTrash("member-1", LocalDateTime.now());
        return file;
    }

    /**
     * 文件仓储假实现。
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
     * 目录查询端口假实现，用于模拟原目录是否存在。
     */
    private static final class FakeDirectoryLookupPort implements DirectoryLookupPort {
        private final List<String> existing = new ArrayList<>();

        void exists(String projectId, String directoryId) {
            existing.add(projectId + "|" + directoryId);
        }

        @Override
        public boolean existsByProjectIdAndDirectoryId(String projectId, String directoryId) {
            return existing.contains(projectId + "|" + directoryId);
        }
    }

    /**
     * 权限检查假实现。
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
            events.add("file.restored:" + directoryId);
        }
    }
}
