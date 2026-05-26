package com.cooperation.application.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.cooperation.domain.log.OperationAction;
import com.cooperation.domain.log.OperationLog;
import com.cooperation.domain.notification.NotificationEventType;
import com.cooperation.domain.project.Project;
import com.cooperation.domain.project.ProjectStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 结束项目应用用例测试。
 */
class EndProjectUseCaseTest {

    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-05-24T10:00:00Z"), ZoneOffset.UTC);

    /**
     * 验证负责人结束项目后锁定写操作，设置记录保留期并通知全组成员。
     */
    @Test
    void shouldEndProjectLockWritesApplyRetentionAndNotifyGroup() {
        FakeProjectRepository projectRepository = new FakeProjectRepository();
        projectRepository.project = Project.restore(1L, 10L, 100L, "课程设计协作", ProjectStatus.ACTIVE);
        FakeOperationLogWriter logWriter = new FakeOperationLogWriter();
        FakeNotificationPublisher notificationPublisher = new FakeNotificationPublisher();
        FakeProjectWriteLockPort writeLockPort = new FakeProjectWriteLockPort();
        EndProjectUseCase useCase = new EndProjectUseCase(
                projectRepository,
                writeLockPort,
                logWriter,
                notificationPublisher,
                fixedClock
        );

        EndProjectUseCase.Result result = useCase.handle(new EndProjectUseCase.Command(1L, 100L));

        assertEquals(ProjectStatus.ENDED, result.project().getStatus());
        assertFalse(result.project().canWrite());
        assertEquals(List.of(1L), writeLockPort.lockedProjectIds);
        assertEquals(OperationAction.PROJECT_ENDED, logWriter.records.get(0).action());
        assertEquals(
                fixedClock.instant().plus(OperationLog.DEFAULT_RETENTION_AFTER_PROJECT_ENDED),
                logWriter.records.get(0).retainUntil()
        );
        assertEquals(NotificationEventType.PROJECT_ENDED, notificationPublisher.events.get(0).type());
    }

    /**
     * 操作记录只暴露测试断言需要的字段。
     */
    private record LogRecord(Long projectId, Long actorId, OperationAction action, Instant retainUntil) {
    }

    /**
     * 通知事件只暴露测试断言需要的字段。
     */
    private record NotificationEvent(Long projectId, NotificationEventType type) {
    }

    /**
     * 项目仓储假实现，避免依赖数据库。
     */
    private static final class FakeProjectRepository implements EndProjectUseCase.ProjectRepository {
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
     * 项目写操作锁定端口假实现，仅记录被锁定的项目。
     */
    private static final class FakeProjectWriteLockPort implements EndProjectUseCase.ProjectWriteLockPort {
        private final List<Long> lockedProjectIds = new ArrayList<>();

        @Override
        public void lockProjectWrites(Long projectId) {
            lockedProjectIds.add(projectId);
        }
    }

    /**
     * 操作记录端口假实现，记录保留期写入结果。
     */
    private static final class FakeOperationLogWriter implements EndProjectUseCase.OperationLogWriter {
        private final List<LogRecord> records = new ArrayList<>();

        @Override
        public void recordProjectEnded(Long projectId, Long actorId, Instant retainUntil) {
            records.add(new LogRecord(projectId, actorId, OperationAction.PROJECT_ENDED, retainUntil));
        }
    }

    /**
     * 通知端口假实现，记录用例发布事件。
     */
    private static final class FakeNotificationPublisher implements EndProjectUseCase.NotificationPublisher {
        private final List<NotificationEvent> events = new ArrayList<>();

        @Override
        public void publishToGroup(Long projectId, NotificationEventType type) {
            events.add(new NotificationEvent(projectId, type));
        }
    }
}
