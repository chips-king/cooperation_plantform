package com.cooperation.application.packageartifact;

import static org.assertj.core.api.Assertions.assertThat;

import com.cooperation.domain.check.CheckIssueType;
import com.cooperation.domain.check.CheckTarget;
import com.cooperation.domain.check.CleanupItem;
import com.cooperation.domain.check.ProjectFileTree;
import com.cooperation.domain.log.OperationAction;
import com.cooperation.domain.log.OperationLog;
import com.cooperation.domain.log.OperationLogRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 打包检查应用用例测试。
 */
class RunPackageCheckUseCaseTest {

    /**
     * 验证打包检查生成风险和清理建议，且所有风险只提醒不阻断打包。
     */
    @Test
    void shouldCreateRiskReportAndCleanupSuggestionWithoutBlockingPackaging() {
        FakeProjectPackageSnapshotRepository snapshots = new FakeProjectPackageSnapshotRepository(ProjectFileTree.of(
                CheckTarget.file("README.md", 1200),
                CheckTarget.file("release/member-submit.zip", 2048),
                CheckTarget.file("__pycache__/service.cpython-312.pyc", 512),
                CheckTarget.file("notes.tmp", 64)
        ));
        FakeOperationLogRepository logs = new FakeOperationLogRepository();
        RunPackageCheckUseCase useCase = new RunPackageCheckUseCase(snapshots, logs);

        RunPackageCheckUseCase.Result result = useCase.run(new RunPackageCheckUseCase.Command("project-1", "leader-1"));

        assertThat(result.canContinuePackaging()).isTrue();
        assertThat(result.report().hasBlockingIssue()).isFalse();
        assertThat(result.report().issues())
                .extracting(issue -> issue.type())
                .contains(CheckIssueType.ARCHIVE_FILE, CheckIssueType.CACHE_FILE, CheckIssueType.TEMPORARY_FILE);
        assertThat(result.cleanupSuggestion().items())
                .extracting(CleanupItem::path)
                .containsExactlyInAnyOrder("__pycache__/service.cpython-312.pyc", "notes.tmp")
                .doesNotContain("release/member-submit.zip");
        assertThat(logs.actions()).containsExactly(OperationAction.CHECK_RUN);
    }

    /**
     * 内存项目快照仓储，避免依赖数据库或真实文件系统。
     */
    private static final class FakeProjectPackageSnapshotRepository implements ProjectPackageSnapshotRepository {

        private final ProjectFileTree tree;

        private FakeProjectPackageSnapshotRepository(ProjectFileTree tree) {
            this.tree = tree;
        }

        @Override
        public ProjectFileTree findCheckTreeByProjectId(String projectId) {
            return tree;
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
