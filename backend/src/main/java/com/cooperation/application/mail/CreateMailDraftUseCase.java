package com.cooperation.application.mail;

import com.cooperation.application.common.ApplicationCommand;
import com.cooperation.application.common.ApplicationResult;
import com.cooperation.domain.log.OperationAction;
import com.cooperation.domain.mail.MailDraft;
import com.cooperation.domain.notification.NotificationEventType;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 创建邮件草稿应用用例。
 */
public class CreateMailDraftUseCase {

    private final PackageRepository packageRepository;
    private final MailDraftRepository mailDraftRepository;
    private final OperationLogWriter operationLogWriter;
    private final NotificationPublisher notificationPublisher;

    /**
     * 创建邮件草稿用例实例。
     *
     * @param packageRepository 压缩包查询仓储
     * @param mailDraftRepository 邮件草稿仓储
     * @param operationLogWriter 操作记录写入端口
     * @param notificationPublisher 通知发布端口
     */
    public CreateMailDraftUseCase(
            PackageRepository packageRepository,
            MailDraftRepository mailDraftRepository,
            OperationLogWriter operationLogWriter,
            NotificationPublisher notificationPublisher
    ) {
        this.packageRepository = Objects.requireNonNull(packageRepository, "压缩包仓储不能为空");
        this.mailDraftRepository = Objects.requireNonNull(mailDraftRepository, "邮件草稿仓储不能为空");
        this.operationLogWriter = Objects.requireNonNull(operationLogWriter, "操作记录端口不能为空");
        this.notificationPublisher = Objects.requireNonNull(notificationPublisher, "通知端口不能为空");
    }

    /**
     * 基于项目最近可用压缩包创建邮件草稿。
     *
     * @param command 创建草稿命令
     * @return 创建草稿结果
     */
    public Result handle(Command command) {
        Objects.requireNonNull(command, "创建草稿命令不能为空");
        LatestPackage latestPackage = packageRepository.findLatestUsableByProjectId(command.projectId())
                .orElseThrow(() -> new IllegalStateException("项目没有可用的最近压缩包"));
        MailDraft draft = MailDraft.create(
                command.projectId(),
                latestPackage.packageId(),
                command.recipients(),
                command.subject(),
                command.body(),
                command.actorId()
        );
        MailDraft savedDraft = mailDraftRepository.save(draft);

        operationLogWriter.record(
                savedDraft.getProjectId(),
                command.actorId(),
                OperationAction.MAIL_DRAFT_CREATED,
                savedDraft.getPackageId()
        );
        notificationPublisher.publishToGroup(savedDraft.getProjectId(), NotificationEventType.MAIL_DRAFT_CREATED);
        return new Result(savedDraft, latestPackage.filename());
    }

    /**
     * 创建邮件草稿命令。
     *
     * @param projectId 项目标识
     * @param actorId 操作人标识
     * @param recipients 收件人邮箱列表
     * @param subject 邮件主题
     * @param body 邮件正文
     */
    public record Command(
            String projectId,
            String actorId,
            List<String> recipients,
            String subject,
            String body
    ) implements ApplicationCommand {
    }

    /**
     * 创建邮件草稿结果。
     *
     * @param draft 已保存的邮件草稿
     * @param attachmentFileName 附件文件名
     */
    public record Result(MailDraft draft, String attachmentFileName) implements ApplicationResult {
    }

    /**
     * 最近可用压缩包摘要。
     *
     * @param packageId 压缩包标识
     * @param filename 压缩包文件名
     */
    public record LatestPackage(String packageId, String filename) {
    }

    /**
     * 创建草稿所需的压缩包查询仓储。
     */
    public interface PackageRepository {

        /**
         * 查询项目最近可作为邮件附件的压缩包。
         *
         * @param projectId 项目标识
         * @return 最近可用压缩包，不存在时为空
         */
        Optional<LatestPackage> findLatestUsableByProjectId(String projectId);
    }

    /**
     * 创建草稿所需的邮件草稿仓储。
     */
    public interface MailDraftRepository {

        /**
         * 保存邮件草稿。
         *
         * @param draft 邮件草稿实体
         * @return 已保存的邮件草稿实体
         */
        MailDraft save(MailDraft draft);
    }

    /**
     * 创建草稿所需的操作记录端口。
     */
    public interface OperationLogWriter {

        /**
         * 记录邮件草稿创建动作。
         *
         * @param projectId 项目标识
         * @param actorId 操作人标识
         * @param action 操作动作
         * @param targetId 操作目标标识
         */
        void record(String projectId, String actorId, OperationAction action, String targetId);
    }

    /**
     * 创建草稿所需的通知发布端口。
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
