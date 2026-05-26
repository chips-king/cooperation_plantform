package com.cooperation.application.directory;

import com.cooperation.application.log.OperationLogWriter;
import com.cooperation.application.notification.NotificationPublisher;
import com.cooperation.application.permission.PermissionChecker;
import com.cooperation.domain.directory.DirectoryNode;
import com.cooperation.domain.directory.DirectoryStatus;
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
 * 目录状态更新应用用例测试，表达权限校验、状态变更和记录通知契约。
 */
class UpdateDirectoryStatusUseCaseTest {

    private final FakeDirectoryRepository directories = new FakeDirectoryRepository();
    private final FakePermissionChecker permissionChecker = new FakePermissionChecker();
    private final FakeOperationLogWriter operationLogs = new FakeOperationLogWriter();
    private final FakeNotificationPublisher notifications = new FakeNotificationPublisher();
    private final UpdateDirectoryStatusUseCase useCase = new UpdateDirectoryStatusUseCase(directories, permissionChecker, operationLogs, notifications);

    /**
     * 验证有目录状态权限的成员可以将目录更新为进行中并写入记录。
     */
    @Test
    void shouldUpdateDirectoryStatusWhenUserHasPermission() {
        directories.save("directory-1", DirectoryNode.create(1L, null, "任务一", 10L));
        permissionChecker.allow("member-1", "project-1", "directory-1", PermissionCode.DIRECTORY_STATUS_UPDATE);

        UpdateDirectoryStatusResult result = useCase.update(new UpdateDirectoryStatusCommand(
                "project-1",
                "directory-1",
                "member-1",
                DirectoryStatus.IN_PROGRESS
        ));

        assertThat(result.directory().getStatus()).isEqualTo(DirectoryStatus.IN_PROGRESS);
        assertThat(result.directory().getStatusChangedBy()).isEqualTo(1L);
        assertThat(operationLogs.actions()).containsExactly(OperationAction.DIRECTORY_STATUS_UPDATE);
        assertThat(operationLogs.last().getMetadata()).containsEntry("nextStatus", "in_progress");
        assertThat(notifications.events).containsExactly("directory.status.updated:directory-1");
    }

    /**
     * 验证负责人拥有目录状态更新权限时可以更新为已完成。
     */
    @Test
    void shouldAllowOwnerToCompleteDirectoryStatus() {
        DirectoryNode directory = DirectoryNode.create(1L, null, "任务一", 10L);
        directory.changeStatus(DirectoryStatus.IN_PROGRESS, 10L);
        directories.save("directory-1", directory);
        permissionChecker.allow("owner-1", "project-1", "directory-1", PermissionCode.DIRECTORY_STATUS_UPDATE);

        UpdateDirectoryStatusResult result = useCase.update(new UpdateDirectoryStatusCommand(
                "project-1",
                "directory-1",
                "owner-1",
                DirectoryStatus.COMPLETED
        ));

        assertThat(result.directory().getStatus()).isEqualTo(DirectoryStatus.COMPLETED);
        assertThat(operationLogs.actions()).containsExactly(OperationAction.DIRECTORY_STATUS_UPDATE);
    }

    /**
     * 验证成员可以将目录状态调回未开始，补齐目录三态的应用层契约。
     */
    @Test
    void shouldAllowMemberToResetDirectoryStatusToNotStarted() {
        DirectoryNode directory = DirectoryNode.create(1L, null, "任务一", 10L);
        directory.changeStatus(DirectoryStatus.IN_PROGRESS, 10L);
        directories.save("directory-1", directory);
        permissionChecker.allow("member-1", "project-1", "directory-1", PermissionCode.DIRECTORY_STATUS_UPDATE);

        UpdateDirectoryStatusResult result = useCase.update(new UpdateDirectoryStatusCommand(
                "project-1",
                "directory-1",
                "member-1",
                DirectoryStatus.NOT_STARTED
        ));

        assertThat(result.directory().getStatus()).isEqualTo(DirectoryStatus.NOT_STARTED);
        assertThat(operationLogs.actions()).containsExactly(OperationAction.DIRECTORY_STATUS_UPDATE);
        assertThat(operationLogs.last().getMetadata()).containsEntry("nextStatus", "not_started");
        assertThat(notifications.events).containsExactly("directory.status.updated:directory-1");
    }

    /**
     * 验证无权限用户不能更新目录状态，也不会写入记录。
     */
    @Test
    void shouldRejectStatusUpdateWhenUserHasNoPermission() {
        directories.save("directory-1", DirectoryNode.create(1L, null, "任务一", 10L));

        assertThatThrownBy(() -> useCase.update(new UpdateDirectoryStatusCommand(
                "project-1",
                "directory-1",
                "readonly-1",
                DirectoryStatus.IN_PROGRESS
        ))).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("没有目录状态更新权限");

        assertThat(operationLogs.logs).isEmpty();
        assertThat(notifications.events).isEmpty();
    }

    /**
     * 目录仓储假实现，仅保存目录状态测试所需数据。
     */
    private static final class FakeDirectoryRepository implements DirectoryRepository {
        private final List<Entry> entries = new ArrayList<>();

        @Override
        public DirectoryNode save(String directoryId, DirectoryNode directory) {
            entries.removeIf(entry -> entry.directoryId.equals(directoryId));
            entries.add(new Entry(directoryId, directory));
            return directory;
        }

        @Override
        public Optional<DirectoryNode> findByProjectIdAndDirectoryId(String projectId, String directoryId) {
            return entries.stream().filter(entry -> entry.directoryId.equals(directoryId)).map(entry -> entry.directory).findFirst();
        }

        private record Entry(String directoryId, DirectoryNode directory) {
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
        public void publishDirectoryStatusChanged(String projectId, String directoryId, DirectoryStatus nextStatus) {
            events.add("directory.status.updated:" + directoryId);
        }
    }
}
