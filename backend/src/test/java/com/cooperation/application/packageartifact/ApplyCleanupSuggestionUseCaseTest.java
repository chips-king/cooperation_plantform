package com.cooperation.application.packageartifact;

import static org.assertj.core.api.Assertions.assertThat;

import com.cooperation.domain.check.CheckIssueType;
import com.cooperation.domain.check.CleanupItem;
import com.cooperation.domain.file.FileAsset;
import com.cooperation.domain.file.FileAssetStatus;
import com.cooperation.domain.file.FileName;
import com.cooperation.domain.log.OperationAction;
import com.cooperation.domain.log.OperationLog;
import com.cooperation.domain.log.OperationLogRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 应用清理建议用例测试。
 */
class ApplyCleanupSuggestionUseCaseTest {

    /**
     * 验证执行清理前可以先预览将进入回收站的对象，避免直接修改文件状态。
     */
    @Test
    void shouldPreviewCleanupObjectsBeforeApplyingSuggestion() {
        FileAsset cacheFile = file("file-1", "project-1", "dir-src", "service.cpython-312.pyc");
        FileAsset tempFile = file("file-2", "project-1", "dir-root", "notes.tmp");
        FakeCleanupTargetRepository targets = new FakeCleanupTargetRepository(Map.of(
                "__pycache__/service.cpython-312.pyc", cacheFile,
                "notes.tmp", tempFile
        ));
        FakeCleanupPermissionChecker permissions = new FakeCleanupPermissionChecker(true);
        ApplyCleanupSuggestionUseCase useCase = new ApplyCleanupSuggestionUseCase(
                targets,
                permissions,
                new FakeOperationLogRepository()
        );

        ApplyCleanupSuggestionUseCase.PreviewResult preview = useCase.preview(new ApplyCleanupSuggestionUseCase.PreviewCommand(
                "project-1",
                "leader-1",
                List.of(
                        new CleanupItem(CheckIssueType.CACHE_FILE, "__pycache__/service.cpython-312.pyc"),
                        new CleanupItem(CheckIssueType.TEMPORARY_FILE, "notes.tmp")
                )
        ));

        assertThat(preview.objects())
                .extracting(ApplyCleanupSuggestionUseCase.PreviewObject::path)
                .containsExactlyInAnyOrder("__pycache__/service.cpython-312.pyc", "notes.tmp");
        assertThat(cacheFile.status()).isEqualTo(FileAssetStatus.ACTIVE);
        assertThat(tempFile.status()).isEqualTo(FileAssetStatus.ACTIVE);
        assertThat(targets.savedIds()).isEmpty();
        assertThat(permissions.checkedPaths())
                .containsExactlyInAnyOrder("__pycache__/service.cpython-312.pyc", "notes.tmp");
    }

    /**
     * 验证清理建议执行后对象进入回收站，并写入清理操作记录。
     */
    @Test
    void shouldMoveSuggestedObjectsToTrashAndRecordCleanupLog() {
        FileAsset cacheFile = file("file-1", "project-1", "dir-src", "service.cpython-312.pyc");
        FileAsset tempFile = file("file-2", "project-1", "dir-root", "notes.tmp");
        FakeCleanupTargetRepository targets = new FakeCleanupTargetRepository(Map.of(
                "__pycache__/service.cpython-312.pyc", cacheFile,
                "notes.tmp", tempFile
        ));
        FakeCleanupPermissionChecker permissions = new FakeCleanupPermissionChecker(true);
        FakeOperationLogRepository logs = new FakeOperationLogRepository();
        ApplyCleanupSuggestionUseCase useCase = new ApplyCleanupSuggestionUseCase(targets, permissions, logs);

        ApplyCleanupSuggestionUseCase.Result result = useCase.apply(new ApplyCleanupSuggestionUseCase.Command(
                "project-1",
                "leader-1",
                List.of(
                        new CleanupItem(CheckIssueType.CACHE_FILE, "__pycache__/service.cpython-312.pyc"),
                        new CleanupItem(CheckIssueType.TEMPORARY_FILE, "notes.tmp")
                )
        ));

        assertThat(result.cleanedObjectIds()).containsExactlyInAnyOrder("file-1", "file-2");
        assertThat(permissions.checkedPaths())
                .containsExactlyInAnyOrder("__pycache__/service.cpython-312.pyc", "notes.tmp");
        assertThat(cacheFile.status()).isEqualTo(FileAssetStatus.TRASHED);
        assertThat(cacheFile.deletedBy()).isEqualTo("leader-1");
        assertThat(tempFile.status()).isEqualTo(FileAssetStatus.TRASHED);
        assertThat(targets.savedIds()).containsExactlyInAnyOrder("file-1", "file-2");
        assertThat(logs.actions()).containsExactly(OperationAction.CLEANUP_APPLIED);
    }

    /**
     * 创建活动文件资产，供清理用例移动到回收站。
     */
    private static FileAsset file(String id, String projectId, String directoryId, String name) {
        return FileAsset.uploaded(
                id,
                projectId,
                directoryId,
                FileName.of(name),
                128,
                "application/octet-stream",
                "storage/" + id,
                "member-1",
                java.time.LocalDateTime.now(),
                id,
                1
        );
    }

    /**
     * 内存清理对象仓储，按项目内路径定位活动对象。
     */
    private static final class FakeCleanupTargetRepository implements CleanupTargetRepository {

        private final Map<String, FileAsset> filesByPath;
        private final List<FileAsset> saved = new ArrayList<>();

        private FakeCleanupTargetRepository(Map<String, FileAsset> filesByPath) {
            this.filesByPath = new LinkedHashMap<>(filesByPath);
        }

        @Override
        public Optional<FileAsset> findActiveFileByProjectIdAndPath(String projectId, String path) {
            FileAsset file = filesByPath.get(path);
            if (file == null || !file.projectId().equals(projectId) || file.status() != FileAssetStatus.ACTIVE) {
                return Optional.empty();
            }
            return Optional.of(file);
        }

        @Override
        public FileAsset save(FileAsset fileAsset) {
            saved.add(fileAsset);
            return fileAsset;
        }

        private List<String> savedIds() {
            return saved.stream().map(FileAsset::id).toList();
        }
    }

    /**
     * 内存权限检查器，表达应用清理前必须校验权限。
     */
    private static final class FakeCleanupPermissionChecker implements CleanupPermissionChecker {

        private final boolean allowed;
        private final List<String> checkedPaths = new ArrayList<>();

        private FakeCleanupPermissionChecker(boolean allowed) {
            this.allowed = allowed;
        }

        @Override
        public void checkCanCleanup(String projectId, String actorId, List<CleanupItem> items) {
            checkedPaths.addAll(items.stream().map(CleanupItem::path).toList());
            if (!allowed) {
                throw new IllegalStateException("无清理权限");
            }
        }

        private List<String> checkedPaths() {
            return checkedPaths;
        }
    }

    /**
     * 内存操作记录仓储，仅保存本测试关心的记录。
     */
    private static final class FakeOperationLogRepository implements OperationLogRepository {

        private final List<OperationLog> logs = new ArrayList<>();

        @Override
        public OperationLog save(OperationLog operationLog) {
            logs.add(operationLog);
            return operationLog;
        }

        private List<OperationAction> actions() {
            return logs.stream().map(OperationLog::getAction).toList();
        }

        @Override
        public List<OperationLog> findByProjectId(String projectId) {
            return logs.stream().filter(log -> log.getProjectId().equals(projectId)).toList();
        }

        @Override
        public List<OperationLog> findByProjectIdAndAction(String projectId, OperationAction action) {
            return logs.stream()
                    .filter(log -> log.getProjectId().equals(projectId) && log.getAction() == action)
                    .toList();
        }

        @Override
        public List<OperationLog> findByProjectIdAndActorId(String projectId, String actorId) {
            return logs.stream()
                    .filter(log -> log.getProjectId().equals(projectId) && log.getActorId().equals(actorId))
                    .toList();
        }

        @Override
        public Optional<OperationLog> findById(String id) {
            return Optional.empty();
        }
    }
}
