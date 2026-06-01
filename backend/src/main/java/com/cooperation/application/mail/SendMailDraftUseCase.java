package com.cooperation.application.mail;

import com.cooperation.application.common.ApplicationCommand;
import com.cooperation.application.common.ApplicationResult;
import com.cooperation.domain.log.OperationAction;
import com.cooperation.domain.mail.MailDraft;
import com.cooperation.domain.notification.NotificationEventType;
import java.time.Clock;
import java.util.Objects;
import java.util.Optional;

/**
 * 发送邮件草稿应用用例。
 */
public class SendMailDraftUseCase {

    private final MailDraftRepository mailDraftRepository;
    private final MailProviderPort mailProviderPort;
    private final OperationLogWriter operationLogWriter;
    private final NotificationPublisher notificationPublisher;
    private final Clock clock;

    /**
     * 创建发送邮件草稿用例实例。
     *
     * @param mailDraftRepository 邮件草稿仓储
     * @param mailProviderPort 邮箱发送端口
     * @param operationLogWriter 操作记录写入端口
     * @param notificationPublisher 通知发布端口
     * @param clock 发送时间时钟
     */
    public SendMailDraftUseCase(
            MailDraftRepository mailDraftRepository,
            MailProviderPort mailProviderPort,
            OperationLogWriter operationLogWriter,
            NotificationPublisher notificationPublisher,
            Clock clock
    ) {
        this.mailDraftRepository = Objects.requireNonNull(mailDraftRepository, "邮件草稿仓储不能为空");
        this.mailProviderPort = Objects.requireNonNull(mailProviderPort, "邮箱发送端口不能为空");
        this.operationLogWriter = Objects.requireNonNull(operationLogWriter, "操作记录端口不能为空");
        this.notificationPublisher = Objects.requireNonNull(notificationPublisher, "通知端口不能为空");
        this.clock = Objects.requireNonNull(clock, "时钟不能为空");
    }

    /**
     * 主动发送指定邮件草稿。
     *
     * @param command 发送草稿命令
     * @return 发送草稿结果
     */
    public Result handle(Command command) {
        Objects.requireNonNull(command, "发送草稿命令不能为空");
        MailDraft draft = mailDraftRepository.findById(command.draftId())
                .orElseThrow(() -> new IllegalStateException("邮件草稿不存在"));
        if (!command.confirmed()) {
            throw new IllegalStateException("发送邮件前必须由负责人确认");
        }

        try {
            mailProviderPort.sendDraft(command.draftId(), draft, command.actorId(), command.smtpConfigId());
        } catch (RuntimeException ex) {
            String failureReason = resolveFailureReason(ex);
            draft.markSendFailed(failureReason);
            mailDraftRepository.save(draft);
            throw new IllegalStateException(failureReason, ex);
        }

        draft.markSent(clock.instant());
        MailDraft savedDraft = mailDraftRepository.save(draft);
        operationLogWriter.record(
                savedDraft.getProjectId(),
                command.actorId(),
                OperationAction.MAIL_SENT,
                command.draftId()
        );
        notificationPublisher.publishToGroup(savedDraft.getProjectId(), NotificationEventType.MAIL_SENT);
        return new Result(savedDraft, "邮件发送成功");
    }

    private String resolveFailureReason(RuntimeException ex) {
        if (ex.getMessage() == null || ex.getMessage().isBlank()) {
            return "邮件发送失败";
        }
        return ex.getMessage();
    }

    /**
     * 发送邮件草稿命令。
     *
     * @param draftId 草稿标识
     * @param actorId 操作人标识
     * @param confirmed 是否已由负责人确认发送
     */
    public record Command(String draftId, String actorId, boolean confirmed, Long smtpConfigId) implements ApplicationCommand {
    }

    /**
     * 发送邮件草稿结果。
     *
     * @param draft 已保存的邮件草稿
     * @param message 发送处理结果提示
     */
    public record Result(MailDraft draft, String message) implements ApplicationResult {
    }

    /**
     * 发送草稿所需的邮件草稿仓储。
     */
    public interface MailDraftRepository {

        /**
         * 按草稿标识查询邮件草稿。
         *
         * @param draftId 草稿标识
         * @return 邮件草稿，不存在时为空
         */
        Optional<MailDraft> findById(String draftId);

        /**
         * 保存邮件草稿。
         *
         * @param draft 邮件草稿实体
         * @return 已保存的邮件草稿实体
         */
        MailDraft save(MailDraft draft);
    }

    /**
     * 邮箱发送端口。
     */
    public interface MailProviderPort {

        /**
         * 发送指定草稿。
         *
         * @param draftId 草稿标识
         * @param draft 草稿实体
         * @param actorId 操作人标识
         * @param smtpConfigId SMTP 配置标识，为空时使用默认配置
         */
        void sendDraft(String draftId, MailDraft draft, String actorId, Long smtpConfigId);
    }

    /**
     * 发送草稿所需的操作记录端口。
     */
    public interface OperationLogWriter {

        /**
         * 记录邮件发送动作。
         *
         * @param projectId 项目标识
         * @param actorId 操作人标识
         * @param action 操作动作
         * @param targetId 操作目标标识
         */
        void record(String projectId, String actorId, OperationAction action, String targetId);
    }

    /**
     * 发送草稿所需的通知发布端口。
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
