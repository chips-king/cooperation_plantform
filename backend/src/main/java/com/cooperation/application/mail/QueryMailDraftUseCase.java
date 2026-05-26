package com.cooperation.application.mail;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * 查询邮件草稿详情的应用层端口。
 */
public interface QueryMailDraftUseCase {

    /**
     * 查询当前用户可见的邮件草稿详情。
     *
     * @param query 邮件草稿查询参数
     * @return 邮件草稿详情
     */
    Result query(Query query);

    /**
     * 邮件草稿查询参数。
     *
     * @param draftId 草稿标识
     * @param actorId 当前用户标识
     */
    record Query(String draftId, String actorId) {

        /**
         * 校验邮件草稿查询参数。
         */
        public Query {
            draftId = requireText(draftId, "草稿标识不能为空");
            actorId = requireText(actorId, "当前用户不能为空");
        }
    }

    /**
     * 邮件草稿详情。
     *
     * @param draftId 草稿标识
     * @param projectId 项目标识
     * @param recipients 收件人列表
     * @param subject 邮件主题
     * @param body 邮件正文
     * @param packageId 附件压缩包标识
     * @param attachmentFilename 附件展示文件名
     * @param status 草稿状态
     * @param createdAt 创建时间
     * @param sentAt 发送时间，未发送时为空
     */
    record Result(
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
         * 校验邮件草稿详情。
         */
        public Result {
            draftId = requireText(draftId, "草稿标识不能为空");
            projectId = requireText(projectId, "项目标识不能为空");
            recipients = List.copyOf(Objects.requireNonNull(recipients, "收件人列表不能为空"));
            subject = Objects.requireNonNullElse(subject, "");
            body = Objects.requireNonNullElse(body, "");
            packageId = requireText(packageId, "压缩包标识不能为空");
            attachmentFilename = requireText(attachmentFilename, "附件文件名不能为空");
            status = requireText(status, "草稿状态不能为空");
            Objects.requireNonNull(createdAt, "创建时间不能为空");
        }
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
