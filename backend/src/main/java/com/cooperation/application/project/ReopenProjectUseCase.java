package com.cooperation.application.project;

import com.cooperation.domain.log.OperationAction;
import com.cooperation.domain.notification.NotificationEventType;
import com.cooperation.domain.project.Project;
import java.util.Objects;
import java.util.Optional;

/**
 * 重新打开项目应用用例，负责恢复协作状态并发布记录和通知。
 */
public final class ReopenProjectUseCase {

    private final ProjectRepository projectRepository;
    private final ProjectWriteLockPort projectWriteLockPort;
    private final OperationLogWriter operationLogWriter;
    private final NotificationPublisher notificationPublisher;

    /**
     * 创建重新打开项目用例实例。
     *
     * @param projectRepository 项目仓储。
     * @param operationLogWriter 操作记录端口。
     * @param notificationPublisher 通知发布端口。
     */
    public ReopenProjectUseCase(
            ProjectRepository projectRepository,
            OperationLogWriter operationLogWriter,
            NotificationPublisher notificationPublisher
    ) {
        this(projectRepository, projectId -> {
        }, operationLogWriter, notificationPublisher);
    }

    /**
     * 创建带项目写锁端口的重新打开项目用例实例。
     *
     * @param projectRepository 项目仓储。
     * @param projectWriteLockPort 项目写操作恢复端口。
     * @param operationLogWriter 操作记录端口。
     * @param notificationPublisher 通知发布端口。
     */
    public ReopenProjectUseCase(
            ProjectRepository projectRepository,
            ProjectWriteLockPort projectWriteLockPort,
            OperationLogWriter operationLogWriter,
            NotificationPublisher notificationPublisher
    ) {
        this.projectRepository = Objects.requireNonNull(projectRepository, "项目仓储不能为空");
        this.projectWriteLockPort = Objects.requireNonNull(projectWriteLockPort, "项目写锁端口不能为空");
        this.operationLogWriter = Objects.requireNonNull(operationLogWriter, "操作记录端口不能为空");
        this.notificationPublisher = Objects.requireNonNull(notificationPublisher, "通知端口不能为空");
    }

    /**
     * 执行重新打开项目。
     *
     * @param command 重新打开命令。
     * @return 重新打开结果。
     */
    public Result handle(Command command) {
        Objects.requireNonNull(command, "重新打开命令不能为空");
        Project project = projectRepository.findById(command.projectId())
                .orElseThrow(() -> new IllegalArgumentException("项目不存在"));
        project.reopen(command.actorId());
        Project saved = projectRepository.save(project);
        projectWriteLockPort.unlockProjectWrites(saved.getId());
        operationLogWriter.record(saved.getId(), command.actorId(), OperationAction.PROJECT_REOPENED);
        notificationPublisher.publishToGroup(saved.getId(), NotificationEventType.PROJECT_REOPENED);
        return new Result(saved);
    }

    /**
     * 重新打开项目命令。
     *
     * @param projectId 项目标识。
     * @param actorId 操作用户标识。
     */
    public record Command(Long projectId, Long actorId) {
    }

    /**
     * 重新打开项目结果。
     *
     * @param project 重新打开后的项目。
     */
    public record Result(Project project) {
    }

    /**
     * 重新打开项目用例所需的项目仓储端口。
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
     * 重新打开项目后的项目写操作恢复端口。
     */
    public interface ProjectWriteLockPort {

        /**
         * 恢复指定项目的写操作。
         *
         * @param projectId 项目标识。
         */
        void unlockProjectWrites(Long projectId);
    }

    /**
     * 重新打开项目用例的操作记录端口。
     */
    public interface OperationLogWriter {

        /**
         * 记录项目动作。
         *
         * @param projectId 项目标识。
         * @param actorId 操作用户标识。
         * @param action 操作动作。
         */
        void record(Long projectId, Long actorId, OperationAction action);
    }

    /**
     * 重新打开项目用例的通知发布端口。
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
