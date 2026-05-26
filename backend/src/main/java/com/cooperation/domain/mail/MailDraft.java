package com.cooperation.domain.mail;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * 邮件草稿领域实体。
 */
public class MailDraft {

    private String id;
    private final String projectId;
    private final String createdBy;
    private String packageId;
    private List<String> recipients;
    private String subject;
    private String body;
    private MailDraftStatus status;
    private Instant sentAt;
    private String lastFailureReason;

    private MailDraft(
            String projectId,
            String packageId,
            List<String> recipients,
            String subject,
            String body,
            String createdBy
    ) {
        this.projectId = requireText(projectId, "项目标识不能为空");
        this.packageId = requireText(packageId, "压缩包标识不能为空");
        this.recipients = requireRecipients(recipients);
        this.subject = Objects.requireNonNullElse(subject, "");
        this.body = Objects.requireNonNullElse(body, "");
        this.createdBy = requireText(createdBy, "创建人不能为空");
        this.status = MailDraftStatus.DRAFT;
    }

    /**
     * 创建绑定最终压缩包的邮件草稿。
     *
     * @param projectId 项目标识
     * @param packageId 绑定的最终压缩包标识
     * @param recipients 收件人邮箱列表
     * @param subject 邮件主题
     * @param body 邮件正文
     * @param createdBy 创建人标识
     * @return 邮件草稿实体
     */
    public static MailDraft create(
            String projectId,
            String packageId,
            List<String> recipients,
            String subject,
            String body,
            String createdBy
    ) {
        return new MailDraft(projectId, packageId, recipients, subject, body, createdBy);
    }

    /**
     * 绑定持久化后的草稿标识。
     *
     * @param id 草稿标识
     * @return 当前邮件草稿实体
     */
    public MailDraft withId(String id) {
        this.id = requireText(id, "草稿标识不能为空");
        return this;
    }

    /**
     * 修改发送前可编辑的草稿字段。
     *
     * @param recipients 新收件人邮箱列表
     * @param subject 新邮件主题
     * @param body 新邮件正文
     * @param packageId 新绑定压缩包标识
     */
    public void update(List<String> recipients, String subject, String body, String packageId) {
        ensureDraftEditable();
        this.recipients = requireRecipients(recipients);
        this.subject = Objects.requireNonNullElse(subject, "");
        this.body = Objects.requireNonNullElse(body, "");
        this.packageId = requireText(packageId, "压缩包标识不能为空");
        this.lastFailureReason = null;
    }

    /**
     * 标记草稿发送成功。
     *
     * @param sentAt 发送成功时间
     */
    public void markSent(Instant sentAt) {
        ensureDraftEditable();
        this.sentAt = Objects.requireNonNull(sentAt, "发送时间不能为空");
        this.status = MailDraftStatus.SENT;
        this.lastFailureReason = null;
    }

    /**
     * 记录发送失败原因并保持草稿状态。
     *
     * @param failureReason 发送失败原因
     */
    public void markSendFailed(String failureReason) {
        ensureDraftEditable();
        this.status = MailDraftStatus.DRAFT;
        this.lastFailureReason = requireText(failureReason, "失败原因不能为空");
    }

    /**
     * 获取项目标识。
     *
     * @return 项目标识
     */
    public String getProjectId() {
        return projectId;
    }

    /**
     * 获取草稿标识。
     *
     * @return 草稿标识，未持久化时为空
     */
    public String getId() {
        return id;
    }

    /**
     * 获取绑定压缩包标识。
     *
     * @return 压缩包标识
     */
    public String getPackageId() {
        return packageId;
    }

    /**
     * 获取收件人邮箱列表。
     *
     * @return 不可变收件人邮箱列表
     */
    public List<String> getRecipients() {
        return recipients;
    }

    /**
     * 获取邮件主题。
     *
     * @return 邮件主题
     */
    public String getSubject() {
        return subject;
    }

    /**
     * 获取邮件正文。
     *
     * @return 邮件正文
     */
    public String getBody() {
        return body;
    }

    /**
     * 获取草稿状态。
     *
     * @return 草稿状态
     */
    public MailDraftStatus getStatus() {
        return status;
    }

    /**
     * 获取草稿创建人。
     *
     * @return 创建人标识
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * 获取发送成功时间。
     *
     * @return 发送成功时间，未发送时为空
     */
    public Instant getSentAt() {
        return sentAt;
    }

    /**
     * 获取最近一次发送失败原因。
     *
     * @return 失败原因，未失败时为空
     */
    public String getLastFailureReason() {
        return lastFailureReason;
    }

    private void ensureDraftEditable() {
        if (status != MailDraftStatus.DRAFT) {
            throw new IllegalStateException("只有草稿状态可以继续修改或发送");
        }
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static List<String> requireRecipients(List<String> recipients) {
        if (recipients == null || recipients.isEmpty()) {
            throw new IllegalArgumentException("收件人不能为空");
        }
        if (recipients.stream().anyMatch(recipient -> recipient == null || recipient.isBlank())) {
            throw new IllegalArgumentException("收件人不能为空");
        }
        return List.copyOf(recipients);
    }
}
