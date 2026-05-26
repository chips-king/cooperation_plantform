package com.cooperation.application.mail;

import com.cooperation.application.common.ApplicationCommand;
import com.cooperation.application.common.ApplicationResult;
import java.util.List;
import java.util.Objects;

/**
 * 第三方邮箱草稿能力端口。
 *
 * <p>本端口只表达外部邮箱系统可以执行的草稿创建、草稿更新和草稿发送能力。
 * 发送前是否已由负责人确认属于应用用例层契约，应在调用本端口之前完成校验。</p>
 */
public interface MailProviderPort {

    /**
     * 在第三方邮箱中创建草稿。
     *
     * @param command 创建第三方草稿命令
     * @return 第三方草稿创建结果
     */
    CreateDraftResult createDraft(CreateDraftCommand command);

    /**
     * 更新第三方邮箱中的既有草稿。
     *
     * @param command 更新第三方草稿命令
     * @return 第三方草稿更新结果
     */
    UpdateDraftResult updateDraft(UpdateDraftCommand command);

    /**
     * 发送第三方邮箱中的既有草稿。
     *
     * @param command 发送第三方草稿命令
     * @return 第三方草稿发送结果
     */
    SendDraftResult sendDraft(SendDraftCommand command);

    /**
     * 创建第三方草稿命令。
     *
     * @param draftId 本系统草稿标识，用于关联第三方草稿
     * @param projectId 项目标识
     * @param packageId 附件压缩包标识
     * @param recipients 收件人邮箱列表
     * @param subject 邮件主题
     * @param body 邮件正文
     * @param attachmentFilename 附件展示文件名
     */
    record CreateDraftCommand(
            String draftId,
            String projectId,
            String packageId,
            List<String> recipients,
            String subject,
            String body,
            String attachmentFilename
    ) implements ApplicationCommand {

        /**
         * 校验创建第三方草稿命令。
         */
        public CreateDraftCommand {
            draftId = requireText(draftId, "草稿标识不能为空");
            projectId = requireText(projectId, "项目标识不能为空");
            packageId = requireText(packageId, "压缩包标识不能为空");
            recipients = requireRecipients(recipients);
            subject = Objects.requireNonNullElse(subject, "");
            body = Objects.requireNonNullElse(body, "");
            attachmentFilename = requireText(attachmentFilename, "附件文件名不能为空");
        }
    }

    /**
     * 创建第三方草稿结果。
     *
     * @param draftId 本系统草稿标识
     * @param providerDraftId 第三方邮箱草稿标识
     */
    record CreateDraftResult(String draftId, String providerDraftId) implements ApplicationResult {

        /**
         * 校验创建第三方草稿结果。
         */
        public CreateDraftResult {
            draftId = requireText(draftId, "草稿标识不能为空");
            providerDraftId = requireText(providerDraftId, "第三方草稿标识不能为空");
        }
    }

    /**
     * 更新第三方草稿命令。
     *
     * @param draftId 本系统草稿标识
     * @param providerDraftId 第三方邮箱草稿标识
     * @param packageId 附件压缩包标识
     * @param recipients 收件人邮箱列表
     * @param subject 邮件主题
     * @param body 邮件正文
     * @param attachmentFilename 附件展示文件名
     */
    record UpdateDraftCommand(
            String draftId,
            String providerDraftId,
            String packageId,
            List<String> recipients,
            String subject,
            String body,
            String attachmentFilename
    ) implements ApplicationCommand {

        /**
         * 校验更新第三方草稿命令。
         */
        public UpdateDraftCommand {
            draftId = requireText(draftId, "草稿标识不能为空");
            providerDraftId = requireText(providerDraftId, "第三方草稿标识不能为空");
            packageId = requireText(packageId, "压缩包标识不能为空");
            recipients = requireRecipients(recipients);
            subject = Objects.requireNonNullElse(subject, "");
            body = Objects.requireNonNullElse(body, "");
            attachmentFilename = requireText(attachmentFilename, "附件文件名不能为空");
        }
    }

    /**
     * 更新第三方草稿结果。
     *
     * @param draftId 本系统草稿标识
     * @param providerDraftId 第三方邮箱草稿标识
     */
    record UpdateDraftResult(String draftId, String providerDraftId) implements ApplicationResult {

        /**
         * 校验更新第三方草稿结果。
         */
        public UpdateDraftResult {
            draftId = requireText(draftId, "草稿标识不能为空");
            providerDraftId = requireText(providerDraftId, "第三方草稿标识不能为空");
        }
    }

    /**
     * 发送第三方草稿命令。
     *
     * @param draftId 本系统草稿标识
     * @param providerDraftId 第三方邮箱草稿标识
     */
    record SendDraftCommand(String draftId, String providerDraftId) implements ApplicationCommand {

        /**
         * 校验发送第三方草稿命令。
         */
        public SendDraftCommand {
            draftId = requireText(draftId, "草稿标识不能为空");
            providerDraftId = requireText(providerDraftId, "第三方草稿标识不能为空");
        }
    }

    /**
     * 发送第三方草稿结果。
     *
     * @param draftId 本系统草稿标识
     * @param providerDraftId 第三方邮箱草稿标识
     * @param providerMessageId 第三方邮箱已发送邮件标识
     */
    record SendDraftResult(
            String draftId,
            String providerDraftId,
            String providerMessageId
    ) implements ApplicationResult {

        /**
         * 校验发送第三方草稿结果。
         */
        public SendDraftResult {
            draftId = requireText(draftId, "草稿标识不能为空");
            providerDraftId = requireText(providerDraftId, "第三方草稿标识不能为空");
            providerMessageId = requireText(providerMessageId, "第三方邮件标识不能为空");
        }
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static List<String> requireRecipients(List<String> recipients) {
        Objects.requireNonNull(recipients, "收件人列表不能为空");
        if (recipients.isEmpty()) {
            throw new IllegalArgumentException("收件人不能为空");
        }
        if (recipients.stream().anyMatch(recipient -> recipient == null || recipient.isBlank())) {
            throw new IllegalArgumentException("收件人不能为空");
        }
        return List.copyOf(recipients);
    }
}
