package com.cooperation.application.mail;

import com.cooperation.application.common.ApplicationCommand;
import com.cooperation.domain.log.OperationAction;
import com.cooperation.domain.notification.NotificationEventType;
import java.util.Objects;

/**
 * 删除邮件草稿应用用例。
 */
public class DeleteMailDraftUseCase {

    private final MailDraftRepository mailDraftRepository;
    private final OperationLogWriter operationLogWriter;
    private final NotificationPublisher notificationPublisher;

    /**
     * 创建删除邮件草稿用例实例。
     *
     * @param mailDraftRepository 邮件草稿仓储
     * @param operationLogWriter 操作记录写入端口
     * @param notificationPublisher 通知发布端口
     */
    public DeleteMailDraftUseCase(
            MailDraftRepository mailDraftRepository,
            OperationLogWriter operationLogWriter,
            NotificationPublisher notificationPublisher
    ) {
        this.mailDraftRepository = Objects.requireNonNull(mailDraftRepository, "邮件草稿仓储不能为空");
        this.operationLogWriter = Objects.requireNonNull(operationLogWriter, "操作记录端口不能为空");
        this.notificationPublisher = Objects.requireNonNull(notificationPublisher, "通知端口不能为空");
    }

    /**
     * 删除指定邮件草稿。
     *
     * @param command 删除草稿命令
     */
    public void handle(Command command) {
        Objects.requireNonNull(command, "删除草稿命令不能为空");
        String projectId = mailDraftRepository.deleteById(command.draftId());
        operationLogWriter.record(projectId, command.actorId(), OperationAction.MAIL_DRAFT_DELETED, command.draftId());
        notificationPublisher.publishToGroup(projectId, NotificationEventType.MAIL_DRAFT_DELETED);
    }

    /**
     * 删除邮件草稿命令。
     *
     * @param draftId 草稿标识
     * @param actorId 操作人标识
     */
    public record Command(String draftId, String actorId) implements ApplicationCommand {
    }

    /**
     * 删除草稿所需的邮件草稿仓储。
     */
    public interface MailDraftRepository {

        /**
         * 按草稿标识删除邮件草稿，返回所属项目标识（用于记录操作日志）。
         *
         * @param draftId 草稿标识
         * @return 草稿所属项目标识
         */
        String deleteById(String draftId);
    }

    /**
     * 删除草稿所需的操作记录端口。
     */
    public interface OperationLogWriter {

        /**
         * 记录邮件草稿删除动作。
         *
         * @param projectId 项目标识
         * @param actorId 操作人标识
         * @param action 操作动作
         * @param targetId 操作目标标识
         */
        void record(String projectId, String actorId, OperationAction action, String targetId);
    }

    /**
     * 删除草稿所需的通知发布端口。
     */
    public interface NotificationPublisher {

        /**
         * 向项目全组成员发布通知。
         *
         * @param projectId 项目标识
         * @param type 通知事件类型
         */
        void publishToGroup(String projectId, NotificationEventType type);
    }
}
