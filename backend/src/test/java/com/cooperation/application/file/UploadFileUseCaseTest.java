package com.cooperation.application.file;

import com.cooperation.application.log.OperationLogWriter;
import com.cooperation.application.notification.NotificationPublisher;
import com.cooperation.application.permission.PermissionChecker;
import com.cooperation.domain.file.DuplicateFilePolicy;
import com.cooperation.domain.file.FileAsset;
import com.cooperation.domain.file.FileAssetRepository;
import com.cooperation.domain.file.FileName;
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
 * 文件上传应用用例测试，表达上传、同名策略和权限校验的应用层契约。
 */
class UploadFileUseCaseTest {

    private final FakeFileAssetRepository files = new FakeFileAssetRepository();
    private final FakeFileStoragePort storage = new FakeFileStoragePort();
    private final FakePermissionChecker permissionChecker = new FakePermissionChecker();
    private final FakeOperationLogWriter operationLogs = new FakeOperationLogWriter();
    private final FakeNotificationPublisher notifications = new FakeNotificationPublisher();
    private final UploadFileUseCase useCase = new UploadFileUseCase(files, storage, permissionChecker, operationLogs, notifications);

    /**
     * 验证成员上传源文件后会保存文件元数据、写入存储并记录上传操作。
     */
    @Test
    void shouldUploadSourceFileAndWriteOperationLog() {
        permissionChecker.allow("member-1", "project-1", "directory-1", PermissionCode.FILE_UPLOAD);

        UploadFileResult result = useCase.upload(new UploadFileCommand(
                "project-1",
                "directory-1",
                "member-1",
                "src/App.java",
                512L,
                "text/x-java-source",
                false,
                DuplicateFilePolicy.RENAME,
                null,
                new byte[]{1, 2, 3}
        ));

        assertThat(result.file().name().value()).isEqualTo("App.java");
        assertThat(result.file().directoryId()).isEqualTo("directory-1");
        assertThat(result.file().uploadedBy()).isEqualTo("member-1");
        assertThat(storage.savedNames).containsExactly("App.java");
        assertThat(operationLogs.actions()).containsExactly(OperationAction.FILE_UPLOAD);
        assertThat(notifications.events).containsExactly("file.uploaded:directory-1");
    }

    /**
     * 验证未显式传同名策略时默认按保留新版本处理，避免日志记录阶段空指针。
     */
    @Test
    void shouldDefaultToNewVersionPolicyWhenDuplicatePolicyIsMissing() {
        permissionChecker.allow("member-1", "project-1", "directory-1", PermissionCode.FILE_UPLOAD);

        UploadFileResult result = useCase.upload(new UploadFileCommand(
                "project-1",
                "directory-1",
                "member-1",
                "notes.txt",
                64L,
                "text/plain",
                false,
                null,
                null,
                new byte[]{1, 2}
        ));

        assertThat(result.file().name().value()).isEqualTo("notes.txt");
        assertThat(operationLogs.last().getMetadata()).containsEntry("duplicatePolicy", "NEW_VERSION");
    }

    /**
     * 验证压缩包可以按普通文件上传，后续检查阶段再提示压缩包风险。
     */
    @Test
    void shouldUploadArchiveFileAsProjectFile() {
        permissionChecker.allow("member-1", "project-1", "directory-1", PermissionCode.FILE_UPLOAD);

        UploadFileResult result = useCase.upload(new UploadFileCommand(
                "project-1",
                "directory-1",
                "member-1",
                "deliverable.zip",
                2048L,
                "application/zip",
                true,
                DuplicateFilePolicy.RENAME,
                null,
                new byte[]{7, 8, 9}
        ));

        assertThat(result.file().name().value()).isEqualTo("deliverable.zip");
        assertThat(result.archive()).isTrue();
        assertThat(files.saved).hasSize(1);
        assertThat(operationLogs.last().getMetadata()).containsEntry("archive", "true");
    }

    /**
     * 验证同名覆盖策略会替换当前文件并保留上传记录中的策略信息。
     */
    @Test
    void shouldApplyOverwritePolicyWhenDuplicateFileExists() {
        permissionChecker.allow("member-1", "project-1", "directory-1", PermissionCode.FILE_UPLOAD);
        FileAsset oldFile = uploadedFile("file-old", "report.docx", "directory-1", "version-1", 1);
        files.save(oldFile);

        UploadFileResult result = useCase.upload(new UploadFileCommand(
                "project-1",
                "directory-1",
                "member-1",
                "report.docx",
                1024L,
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                false,
                DuplicateFilePolicy.OVERWRITE,
                null,
                new byte[]{4, 5, 6}
        ));

        assertThat(oldFile.status().name()).isEqualTo("SUPERSEDED");
        assertThat(result.file().name().value()).isEqualTo("report.docx");
        assertThat(operationLogs.last().getMetadata()).containsEntry("duplicatePolicy", "OVERWRITE");
    }

    /**
     * 验证同名重命名策略要求使用用户选择的新名称。
     */
    @Test
    void shouldApplyRenamePolicyWhenDuplicateFileExists() {
        permissionChecker.allow("member-1", "project-1", "directory-1", PermissionCode.FILE_UPLOAD);
        files.save(uploadedFile("file-old", "report.docx", "directory-1", "version-1", 1));

        UploadFileResult result = useCase.upload(new UploadFileCommand(
                "project-1",
                "directory-1",
                "member-1",
                "report.docx",
                1024L,
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                false,
                DuplicateFilePolicy.RENAME,
                "report (1).docx",
                new byte[]{4, 5, 6}
        ));

        assertThat(result.file().name().value()).isEqualTo("report (1).docx");
        assertThat(files.saved).extracting(file -> file.name().value()).contains("report.docx", "report (1).docx");
    }

    /**
     * 验证同名保留新版本策略会复用版本组并递增版本号。
     */
    @Test
    void shouldApplyNewVersionPolicyWhenDuplicateFileExists() {
        permissionChecker.allow("member-1", "project-1", "directory-1", PermissionCode.FILE_UPLOAD);
        FileAsset oldFile = uploadedFile("file-old", "report.docx", "directory-1", "version-1", 1);
        files.save(oldFile);

        UploadFileResult result = useCase.upload(new UploadFileCommand(
                "project-1",
                "directory-1",
                "member-1",
                "report.docx",
                1024L,
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                false,
                DuplicateFilePolicy.NEW_VERSION,
                null,
                new byte[]{4, 5, 6}
        ));

        assertThat(result.file().versionGroupId()).isEqualTo(oldFile.versionGroupId());
        assertThat(result.file().versionNo()).isEqualTo(2);
        assertThat(operationLogs.last().getMetadata()).containsEntry("duplicatePolicy", "NEW_VERSION");
    }

    /**
     * 验证只读用户没有上传权限时不会保存文件或写入记录。
     */
    @Test
    void shouldRejectReadonlyUserUpload() {
        permissionChecker.deny("readonly-1", "project-1", "directory-1", PermissionCode.FILE_UPLOAD);

        assertThatThrownBy(() -> useCase.upload(new UploadFileCommand(
                "project-1",
                "directory-1",
                "readonly-1",
                "readonly.txt",
                128L,
                "text/plain",
                false,
                DuplicateFilePolicy.RENAME,
                null,
                new byte[]{1}
        ))).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("没有上传权限");

        assertThat(files.saved).isEmpty();
        assertThat(operationLogs.logs).isEmpty();
    }

    /**
     * 创建文件测试样本，避免测试依赖真实文件系统。
     */
    private FileAsset uploadedFile(String id, String name, String directoryId, String versionGroupId, int versionNo) {
        return FileAsset.uploaded(
                id,
                "project-1",
                directoryId,
                FileName.of(name),
                1024L,
                "application/octet-stream",
                "project-files/" + id,
                "member-1",
                versionGroupId,
                versionNo
        );
    }

    /**
     * 文件仓储假实现，仅保存测试关心的文件元数据。
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
            return saved.stream()
                    .filter(FileAsset::isActive)
                    .filter(file -> file.directoryId().equals(directoryId))
                    .filter(file -> file.name().value().equals(name.value()))
                    .findFirst();
        }

        @Override
        public List<FileAsset> findActiveByProjectId(String projectId) {
            return saved.stream().filter(FileAsset::isActive).filter(file -> file.projectId().equals(projectId)).toList();
        }

        @Override
        public List<FileAsset> findTrashedByProjectId(String projectId) {
            return saved.stream().filter(file -> file.projectId().equals(projectId)).filter(file -> file.status().name().equals("TRASHED")).toList();
        }
    }

    /**
     * 文件存储端口假实现，仅记录保存请求。
     */
    private static final class FakeFileStoragePort implements FileStoragePort {
        private final List<String> savedNames = new ArrayList<>();

        @Override
        public String save(String projectId, String directoryId, String filename, byte[] content) {
            savedNames.add(filename);
            return "project-files/" + projectId + "/" + filename;
        }
    }

    /**
     * 权限检查假实现，按测试显式配置返回授权结果。
     */
    private static final class FakePermissionChecker implements PermissionChecker {
        private final List<String> allowed = new ArrayList<>();

        void allow(String userId, String projectId, String directoryId, PermissionCode permissionCode) {
            allowed.add(key(userId, projectId, directoryId, permissionCode));
        }

        void deny(String userId, String projectId, String directoryId, PermissionCode permissionCode) {
            allowed.remove(key(userId, projectId, directoryId, permissionCode));
        }

        @Override
        public boolean hasDirectoryPermission(String userId, String projectId, String directoryId, PermissionCode permissionCode) {
            return allowed.contains(key(userId, projectId, directoryId, permissionCode));
        }

        private String key(String userId, String projectId, String directoryId, PermissionCode permissionCode) {
            return userId + "|" + projectId + "|" + directoryId + "|" + permissionCode.code();
        }
    }

    /**
     * 操作记录写入假实现，便于断言上传记录。
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
     * 通知发布假实现，仅记录事件名称。
     */
    private static final class FakeNotificationPublisher implements NotificationPublisher {
        private final List<String> events = new ArrayList<>();

        @Override
        public void publishFileChanged(String projectId, String directoryId, String fileId, OperationAction action) {
            events.add("file.uploaded:" + directoryId);
        }
    }
}
