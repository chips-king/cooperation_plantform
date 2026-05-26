package com.cooperation.application.project;

import com.cooperation.domain.log.OperationAction;
import com.cooperation.domain.log.OperationLog;
import com.cooperation.domain.notification.NotificationEventType;
import com.cooperation.domain.project.Project;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * 结束项目应用用例，负责结束项目、设置记录保留期并发布全组通知。
 */
public final class EndProjectUseCase {

    private final ProjectRepository projectRepository;
    private final ProjectWriteLockPort projectWriteLockPort;
    private final OperationLogWriter operationLogWriter;
    private final NotificationPublisher notificationPublisher;
    private final Clock clock;

    /**
     * 创建结束项目用例实例。
     *
     * @param projectRepository 项目仓储。
     * @param operationLogWriter 操作记录写入端口。
     * @param notificationPublisher 通知发布端口。
     * @param clock 应用时钟。
     */
    public EndProjectUseCase(
            ProjectRepository projectRepository,
            OperationLogWriter operationLogWriter,
            NotificationPublisher notificationPublisher,
            Clock clock
    ) {
        this(projectRepository, projectId -> {
        }, operationLogWriter, notificationPublisher, clock);
    }

    /**
     * 创建带项目写锁端口的结束项目用例实例。
     *
     * @param projectRepository 项目仓储。
     * @param projectWriteLockPort 项目写操作锁定端口。
     * @param operationLogWriter 操作记录写入端口。
     * @param notificationPublisher 通知发布端口。
     * @param clock 应用时钟。
     */
    public EndProjectUseCase(
            ProjectRepository projectRepository,
            ProjectWriteLockPort projectWriteLockPort,
            OperationLogWriter operationLogWriter,
            NotificationPublisher notificationPublisher,
            Clock clock
    ) {
        this.projectRepository = Objects.requireNonNull(projectRepository, "项目仓储不能为空");
        this.projectWriteLockPort = Objects.requireNonNull(projectWriteLockPort, "项目写锁端口不能为空");
        this.operationLogWriter = Objects.requireNonNull(operationLogWriter, "操作记录端口不能为空");
        this.notificationPublisher = Objects.requireNonNull(notificationPublisher, "通知端口不能为空");
        this.clock = Objects.requireNonNull(clock, "应用时钟不能为空");
    }

    /**
     * 执行结束项目。
     *
     * @param command 结束项目命令。
     * @return 结束项目结果。
     */
    public Result handle(Command command) {
        Objects.requireNonNull(command, "结束项目命令不能为空");
        Project project = projectRepository.findById(command.projectId())
                .orElseThrow(() -> new IllegalArgumentException("项目不存在"));
        project.end(command.actorId());
        Project saved = projectRepository.save(project);
        projectWriteLockPort.lockProjectWrites(saved.getId());
        Instant retainUntil = clock.instant().plus(OperationLog.DEFAULT_RETENTION_AFTER_PROJECT_ENDED);
        operationLogWriter.recordProjectEnded(saved.getId(), command.actorId(), retainUntil);
        notificationPublisher.publishToGroup(saved.getId(), NotificationEventType.PROJECT_ENDED);
        return new Result(saved);
    }

    /**
     * 结束项目命令。
     *
     * @param projectId 项目标识。
     * @param actorId 操作用户标识。
     */
    public record Command(Long projectId, Long actorId) {
    }

    /**
     * 结束项目结果。
     *
     * @param project 结束后的项目。
     */
    public record Result(Project project) {
    }

    /**
     * 结束项目用例所需的项目仓储端口。
     */
    public interface ProjectRepository {

        /**
         * 按项目标识查询项目。
         *
         * @param projectId 项目标识。
         * @return 找到时返回项目，否则返回空。
         */
        Optional<Project> findById(Long projectId);

        /**
         * 保存项目。
         *
         * @param project 待保存项目。
         * @return 保存后的项目。
         */
        Project save(Project project);
    }

    /**
     * 结束项目后的项目写操作锁定端口。
     */
    public interface ProjectWriteLockPort {

        /**
         * 锁定指定项目的写操作。
         *
         * @param projectId 项目标识。
         */
        void lockProjectWrites(Long projectId);
    }

    /**
     * 结束项目用例的操作记录端口。
     */
    public interface OperationLogWriter {

        /**
         * 记录项目结束动作及保留到期时间。
         *
         * @param projectId 项目标识。
         * @param actorId 操作用户标识。
         * @param retainUntil 操作记录保留到期时间。
         */
        void recordProjectEnded(Long projectId, Long actorId, Instant retainUntil);
    }

    /**
     * 结束项目用例的通知发布端口。
     */
    public interface NotificationPublisher {

        /**
         * 向项目全组成员发布通知。
         *
         * @param projectId 项目标识。
         * @param type 通知事件类型。
         */
        void publishToGroup(Long projectId, NotificationEventType type);
    }
}
