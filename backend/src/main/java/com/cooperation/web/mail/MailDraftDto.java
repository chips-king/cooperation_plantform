package com.cooperation.web.mail;

import com.cooperation.application.mail.QueryMailDraftUseCase;
import com.cooperation.application.mail.UpdateMailDraftUseCase;
import com.cooperation.domain.mail.MailDraft;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

/**
 * 邮件草稿 Web API 数据传输对象集合。
 */
public final class MailDraftDto {

    private MailDraftDto() {
    }

    /**
     * 创建邮件草稿请求。
     *
     * @param recipients 收件人邮箱列表
     * @param subject 邮件主题
     * @param body 邮件正文
     */
    public record CreateMailDraftRequest(
            @NotEmpty(message = "收件人不能为空") List<@NotBlank(message = "收件人不能为空") @Email(message = "收件人邮箱格式不正确") String> recipients,
            @NotBlank(message = "邮件主题不能为空") String subject,
            @NotBlank(message = "邮件正文不能为空") String body
    ) {
    }

    /**
     * 更新邮件草稿请求。
     *
     * @param recipients 收件人邮箱列表
     * @param subject 邮件主题
     * @param body 邮件正文
     * @param packageId 附件压缩包标识
     */
    public record UpdateMailDraftRequest(
            @NotEmpty(message = "收件人不能为空") List<@NotBlank(message = "收件人不能为空") @Email(message = "收件人邮箱格式不正确") String> recipients,
            @NotBlank(message = "邮件主题不能为空") String subject,
            @NotBlank(message = "邮件正文不能为空") String body,
            @NotBlank(message = "压缩包标识不能为空") String packageId
    ) {
    }

    /**
     * 发送邮件草稿请求。
     *
     * @param confirmed 是否已确认发送
     * @param smtpConfigId SMTP 配置标识，为空时使用默认配置
     */
    public record SendMailDraftRequest(
            @NotNull(message = "发送确认不能为空") Boolean confirmed,
            Long smtpConfigId
    ) {
    }

    /**
     * 邮件草稿详情响应。
     *
     * @param draftId 草稿标识
     * @param projectId 项目标识
     * @param recipients 收件人邮箱列表
     * @param subject 邮件主题
     * @param body 邮件正文
     * @param packageId 附件压缩包标识
     * @param attachmentFilename 附件展示文件名
     * @param status 草稿状态
     * @param createdAt 创建时间
     * @param sentAt 发送时间，未发送时为空
     */
    public record MailDraftResponse(
            String draftId,
            String projectId,
            List<String> recipients,
            String subject,
            String body,
            String packageId,
            String attachmentFilename,
            String status,
            Instant createdAt,
            Instant sentAt
    ) {

        /**
         * 从邮件草稿实体创建响应。
         *
         * @param draft 邮件草稿实体
         * @param attachmentFilename 附件展示文件名
         * @return 邮件草稿详情响应
         */
        public static MailDraftResponse fromCreatedDraft(MailDraft draft, String attachmentFilename) {
            return new MailDraftResponse(
                    draft.getId() == null ? "draft-001" : draft.getId(),
                    draft.getProjectId(),
                    draft.getRecipients(),
                    draft.getSubject(),
                    draft.getBody(),
                    draft.getPackageId(),
                    attachmentFilename,
                    toStatusValue(draft),
                    null,
                    draft.getSentAt()
            );
        }

        /**
         * 从查询草稿结果创建响应。
         *
         * @param result 查询草稿结果
         * @return 邮件草稿详情响应
         */
        public static MailDraftResponse from(QueryMailDraftUseCase.Result result) {
            return new MailDraftResponse(
                    result.draftId(),
                    result.projectId(),
                    result.recipients(),
                    result.subject(),
                    result.body(),
                    result.packageId(),
                    result.attachmentFilename(),
                    result.status(),
                    result.createdAt(),
                    result.sentAt()
            );
        }

        /**
         * 从更新草稿结果创建响应。
         *
         * @param result 更新草稿结果
         * @return 邮件草稿详情响应
         */
        public static MailDraftResponse from(UpdateMailDraftUseCase.Result result) {
            return new MailDraftResponse(
                    result.draftId(),
                    result.projectId(),
                    result.recipients(),
                    result.subject(),
                    result.body(),
                    result.packageId(),
                    result.attachmentFilename(),
                    result.status(),
                    result.createdAt(),
                    null
            );
        }

        /**
         * 从发送草稿结果创建响应。
         *
         * @param draftId 草稿标识
         * @param draft 邮件草稿实体
         * @return 邮件草稿详情响应
         */
        public static MailDraftResponse fromSentDraft(String draftId, MailDraft draft) {
            return new MailDraftResponse(
                    draftId,
                    draft.getProjectId(),
                    draft.getRecipients(),
                    draft.getSubject(),
                    draft.getBody(),
                    draft.getPackageId(),
                    null,
                    toStatusValue(draft),
                    null,
                    draft.getSentAt()
            );
        }
    }

    /**
     * 发送邮件草稿响应。
     *
     * @param draftId 草稿标识
     * @param projectId 项目标识
     * @param recipients 收件人邮箱列表
     * @param subject 邮件主题
     * @param body 邮件正文
     * @param packageId 附件压缩包标识
     * @param attachmentFilename 附件展示文件名
     * @param status 草稿状态
     * @param createdAt 创建时间
     * @param sentAt 发送时间
     * @param message 发送结果提示
     */
    public record SendMailDraftResponse(
            String draftId,
            String projectId,
            List<String> recipients,
            String subject,
            String body,
            String packageId,
            String attachmentFilename,
            String status,
            Instant createdAt,
            Instant sentAt,
            String message
    ) {

        /**
         * 从发送草稿结果创建响应。
         *
         * @param draftId 草稿标识
         * @param draft 邮件草稿实体
         * @param message 发送结果提示
         * @return 发送邮件草稿响应
         */
        public static SendMailDraftResponse from(String draftId, MailDraft draft, String message) {
            MailDraftResponse response = MailDraftResponse.fromSentDraft(draftId, draft);
            return new SendMailDraftResponse(
                    response.draftId(),
                    response.projectId(),
                    response.recipients(),
                    response.subject(),
                    response.body(),
                    response.packageId(),
                    response.attachmentFilename(),
                    response.status(),
                    response.createdAt(),
                    response.sentAt(),
                    message
            );
        }
    }

    /**
     * 将草稿状态转换为接口展示值。
     *
     * @param draft 邮件草稿实体
     * @return 小写草稿状态值
     */
    private static String toStatusValue(MailDraft draft) {
        return draft.getStatus().name().toLowerCase();
    }

    /**
     * 项目草稿概览响应（用户维度汇总）。
     *
     * @param projectId 项目标识
     * @param projectName 项目名称
     * @param draftCount 草稿数量
     * @param latestPackageFilename 最近最终压缩包文件名，未打包时为空
     */
    public record DraftSummaryResponse(
            String projectId,
            String projectName,
            long draftCount,
            String latestPackageFilename
    ) {
    }

    /**
     * 项目内草稿列表项响应。
     *
     * @param draftId 草稿标识
     * @param subject 邮件主题
     * @param status 草稿状态
     * @param createdAt 创建时间
     * @param sentAt 发送时间，未发送时为空
     */
    public record ProjectDraftListItemResponse(
            String draftId,
            String subject,
            String status,
            Instant createdAt,
            Instant sentAt
    ) {
    }
}
