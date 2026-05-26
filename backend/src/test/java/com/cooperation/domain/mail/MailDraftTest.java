package com.cooperation.domain.mail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 邮件草稿领域规则测试。
 */
class MailDraftTest {

    /**
     * 验证创建草稿时会绑定项目、压缩包、收件人和正文信息。
     */
    @Test
    void shouldCreateDraftWithPackageAndRecipients() {
        MailDraft draft = MailDraft.create(
                "project-1",
                "package-1",
                List.of("teacher@example.com"),
                "分工协作成果提交",
                "附件为本次项目最终压缩包。",
                "owner-1"
        );

        assertEquals("project-1", draft.getProjectId());
        assertEquals("package-1", draft.getPackageId());
        assertEquals(List.of("teacher@example.com"), draft.getRecipients());
        assertEquals(MailDraftStatus.DRAFT, draft.getStatus());
    }

    /**
     * 验证草稿可在发送前修改收件人、主题、正文和绑定压缩包。
     */
    @Test
    void shouldUpdateEditableDraftFieldsBeforeSending() {
        MailDraft draft = MailDraft.create(
                "project-1",
                "package-1",
                List.of("old@example.com"),
                "旧主题",
                "旧正文",
                "owner-1"
        );

        draft.update(
                List.of("new@example.com", "reviewer@example.com"),
                "新主题",
                "新正文",
                "package-2"
        );

        assertEquals(List.of("new@example.com", "reviewer@example.com"), draft.getRecipients());
        assertEquals("新主题", draft.getSubject());
        assertEquals("新正文", draft.getBody());
        assertEquals("package-2", draft.getPackageId());
        assertEquals(MailDraftStatus.DRAFT, draft.getStatus());
    }

    /**
     * 验证发送成功后草稿进入已发送状态并记录发送时间。
     */
    @Test
    void shouldMarkDraftAsSentWhenSendSucceeds() {
        MailDraft draft = MailDraft.create(
                "project-1",
                "package-1",
                List.of("teacher@example.com"),
                "分工协作成果提交",
                "附件为本次项目最终压缩包。",
                "owner-1"
        );
        Instant sentAt = Instant.parse("2026-05-24T08:00:00Z");

        draft.markSent(sentAt);

        assertEquals(MailDraftStatus.SENT, draft.getStatus());
        assertEquals(sentAt, draft.getSentAt());
    }

    /**
     * 验证发送失败时草稿保持未发送，方便用户修正后再次发送。
     */
    @Test
    void shouldKeepDraftStatusWhenSendFails() {
        MailDraft draft = MailDraft.create(
                "project-1",
                "package-1",
                List.of("teacher@example.com"),
                "分工协作成果提交",
                "附件为本次项目最终压缩包。",
                "owner-1"
        );

        draft.markSendFailed("邮箱服务暂不可用");

        assertEquals(MailDraftStatus.DRAFT, draft.getStatus());
        assertEquals("邮箱服务暂不可用", draft.getLastFailureReason());
    }

    /**
     * 验证草稿必须绑定最终压缩包。
     */
    @Test
    void shouldRejectDraftWithoutPackage() {
        assertThrows(IllegalArgumentException.class, () -> MailDraft.create(
                "project-1",
                "",
                List.of("teacher@example.com"),
                "分工协作成果提交",
                "附件为本次项目最终压缩包。",
                "owner-1"
        ));
    }

    /**
     * 验证草稿收件人不能为空。
     */
    @Test
    void shouldRejectDraftWithoutRecipients() {
        assertThrows(IllegalArgumentException.class, () -> MailDraft.create(
                "project-1",
                "package-1",
                List.of(),
                "分工协作成果提交",
                "附件为本次项目最终压缩包。",
                "owner-1"
        ));
    }
}
