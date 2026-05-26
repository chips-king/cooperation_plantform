package com.cooperation.application.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cooperation.domain.log.OperationAction;
import com.cooperation.domain.notification.NotificationEventType;
import com.cooperation.domain.project.Project;
import com.cooperation.domain.project.ProjectStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 重新打开项目应用用例测试。
 */
class ReopenProjectUseCaseTest {

    /**
     * 验证负责人重新打开项目后恢复协作状态，并写入记录和全组通知。
     */
    @Test
    void shouldReopenEndedProjectRecordAndNotifyGroup() {
        FakeProjectRepository projectRepository = new FakeProjectRepository();
        projectRepository.project = Project.restore(1L, 10L, 100L, "课程设计协作", ProjectStatus.ENDED);
        FakeOperationLogWriter logWriter = new FakeOperationLogWriter();
        FakeNotificationPublisher notificationPublisher = new FakeNotificationPublisher();
        FakeProjectWriteLockPort writeLockPort = new FakeProjectWriteLockPort();
        ReopenProjectUseCase useCase = new ReopenProjectUseCase(
                projectRepository,
                writeLockPort,
                logWriter,
                notificationPublisher
        );

        ReopenProjectUseCase.Result result = useCase.handle(new ReopenProjectUseCase.Command(1L, 100L));

        assertEquals(ProjectStatus.ACTIVE, result.project().getStatus());
        assertTrue(result.project().canWrite());
        assertEquals(List.of(1L), writeLockPort.unlockedProjectIds);
        assertEquals(OperationAction.PROJECT_REOPENED, logWriter.records.get(0).action());
        assertEquals(NotificationEventType.PROJECT_REOPENED, notificationPublisher.events.get(0).type());
    }

    /**
     * 操作记录只暴露测试断言需要的字段。
     */
    private record LogRecord(Long projectId, Long actorId, OperationAction action) {
    }

    /**
     * 通知事件只暴露测试断言需要的字段。
     */
    private record NotificationEvent(Long projectId, NotificationEventType type) {
    }

    /**
     * 项目仓储假实现，避免依赖数据库。
     */
    private static final class FakeProjectRepository implements ReopenProjectUseCase.ProjectRepository {
        private Project project;

        @Override
        public Optional<Project> findById(Long projectId) {
            return Optional.ofNullable(project);
        }

        @Override
        public Project save(Project project) {
            this.project = project;
            return project;
        }
    }

    /**
     * 项目写操作解锁端口假实现，仅记录恢复协作的项目。
     */
    private static final class FakeProjectWriteLockPort implements ReopenProjectUseCase.ProjectWriteLockPort {
        private final List<Long> unlockedProjectIds = new ArrayList<>();

        @Override
        public void unlockProjectWrites(Long projectId) {
            unlockedProjectIds.add(projectId);
        }
    }

    /**
     * 操作记录端口假实现，记录用例写入动作。
     */
    private static final class FakeOperationLogWriter implements ReopenProjectUseCase.OperationLogWriter {
        private final List<LogRecord> records = new ArrayList<>();

        @Override
        public void record(Long projectId, Long actorId, OperationAction action) {
            records.add(new LogRecord(projectId, actorId, action));
        }
    }

    /**
     * 通知端口假实现，记录用例发布事件。
     */
    private static final class FakeNotificationPublisher implements ReopenProjectUseCase.NotificationPublisher {
        private final List<NotificationEvent> events = new ArrayList<>();

        @Override
        public void publishToGroup(Long projectId, NotificationEventType type) {
            events.add(new NotificationEvent(projectId, type));
        }
    }
}
