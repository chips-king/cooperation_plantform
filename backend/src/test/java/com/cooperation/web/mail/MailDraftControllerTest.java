package com.cooperation.web.mail;

import com.cooperation.application.mail.CreateMailDraftUseCase;
import com.cooperation.application.mail.QueryMailDraftUseCase;
import com.cooperation.application.mail.SendMailDraftUseCase;
import com.cooperation.application.mail.UpdateMailDraftUseCase;
import com.cooperation.domain.mail.MailDraft;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 邮件草稿 Web API 契约测试。
 */
@WebMvcTest(MailDraftController.class)
@AutoConfigureMockMvc(addFilters = false)
class MailDraftControllerTest {

    /** 项目标识用于创建草稿时绑定项目和最近压缩包。 */
    private static final String PROJECT_ID = "project-001";

    /** 草稿标识用于查询、修改和发送同一草稿。 */
    private static final String DRAFT_ID = "draft-001";

    /** 当前操作者用于表达发送和修改接口的用户身份透传。 */
    private static final String ACTOR_ID = "user-owner";

    /** 草稿创建时间用于校验详情响应的稳定字段。 */
    private static final Instant CREATED_AT = Instant.parse("2026-05-24T13:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreateMailDraftUseCase createMailDraftUseCase;

    @MockBean
    private QueryMailDraftUseCase queryMailDraftUseCase;

    @MockBean
    private UpdateMailDraftUseCase updateMailDraftUseCase;

    @MockBean
    private SendMailDraftUseCase sendMailDraftUseCase;

    /**
     * 创建草稿应返回附件展示名 attachmentFilename，便于前端核对最近压缩包。
     */
    @Test
    @DisplayName("创建邮件草稿返回附件展示文件名")
    void shouldCreateMailDraftWithAttachmentFilename() throws Exception {
        given(createMailDraftUseCase.handle(any(CreateMailDraftUseCase.Command.class)))
                .willReturn(new CreateMailDraftUseCase.Result(
                        createDraft("teacher@example.com"),
                        "final-report.zip"
                ));

        mockMvc.perform(post("/projects/{projectId}/mail-drafts", PROJECT_ID)
                        .header("X-User-Id", ACTOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recipients": ["teacher@example.com"],
                                  "subject": "课程作业提交",
                                  "body": "请查收附件。"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data.draftId").value(DRAFT_ID))
                .andExpect(jsonPath("$.data.attachmentFilename").value("final-report.zip"))
                .andExpect(jsonPath("$.data.status").value("draft"));
    }

    /**
     * 查询草稿应返回统一成功结构，并包含收件人、正文和附件文件名。
     */
    @Test
    @DisplayName("查询邮件草稿返回草稿详情")
    void shouldReturnMailDraftDetail() throws Exception {
        given(queryMailDraftUseCase.query(any(QueryMailDraftUseCase.Query.class)))
                .willReturn(new QueryMailDraftUseCase.Result(
                        DRAFT_ID,
                        PROJECT_ID,
                        List.of("teacher@example.com"),
                        "课程作业提交",
                        "请查收附件。",
                        "package-001",
                        "final-report.zip",
                        "draft",
                        CREATED_AT,
                        null
                ));

        mockMvc.perform(get("/mail-drafts/{draftId}", DRAFT_ID)
                        .header("X-User-Id", ACTOR_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.data.draftId").value(DRAFT_ID))
                .andExpect(jsonPath("$.data.recipients[0]").value("teacher@example.com"))
                .andExpect(jsonPath("$.data.attachmentFilename").value("final-report.zip"))
                .andExpect(jsonPath("$.data.createdAt").value(CREATED_AT.toString()));
    }

    /**
     * 更新草稿应支持收件人、主题、正文和附件压缩包调整。
     */
    @Test
    @DisplayName("更新邮件草稿返回修改后的草稿")
    void shouldUpdateMailDraft() throws Exception {
        given(updateMailDraftUseCase.update(any(UpdateMailDraftUseCase.Command.class)))
                .willReturn(new UpdateMailDraftUseCase.Result(
                        DRAFT_ID,
                        PROJECT_ID,
                        List.of("teacher@example.com", "assistant@example.com"),
                        "课程作业最终版",
                        "请查收最终版附件。",
                        "package-002",
                        "final-report-v2.zip",
                        "draft",
                        CREATED_AT
                ));

        mockMvc.perform(patch("/mail-drafts/{draftId}", DRAFT_ID)
                        .header("X-User-Id", ACTOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recipients": ["teacher@example.com", "assistant@example.com"],
                                  "subject": "课程作业最终版",
                                  "body": "请查收最终版附件。",
                                  "packageId": "package-002"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.data.subject").value("课程作业最终版"))
                .andExpect(jsonPath("$.data.packageId").value("package-002"))
                .andExpect(jsonPath("$.data.attachmentFilename").value("final-report-v2.zip"));
    }

    /**
     * 发送草稿必须携带 confirmed=true，成功后返回发送提示 message。
     */
    @Test
    @DisplayName("确认发送邮件草稿返回发送成功提示")
    void shouldSendMailDraftWhenConfirmed() throws Exception {
        given(sendMailDraftUseCase.handle(any(SendMailDraftUseCase.Command.class)))
                .willReturn(new SendMailDraftUseCase.Result(
                        sentDraft(),
                        "邮件发送成功"
                ));

        mockMvc.perform(post("/mail-drafts/{draftId}/send", DRAFT_ID)
                        .header("X-User-Id", ACTOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "confirmed": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.data.draftId").value(DRAFT_ID))
                .andExpect(jsonPath("$.data.status").value("sent"))
                .andExpect(jsonPath("$.data.message").value("邮件发送成功"));
    }

    /**
     * 未确认发送时应返回统一错误结构，并阻止 Controller 进入真实发送流程。
     */
    @Test
    @DisplayName("未确认发送邮件草稿返回冲突错误")
    void shouldReturnConflictWhenSendNotConfirmed() throws Exception {
        mockMvc.perform(post("/mail-drafts/{draftId}/send", DRAFT_ID)
                        .header("X-User-Id", ACTOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "confirmed": false
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    /**
     * 收件人为空时应返回统一校验错误结构。
     */
    @Test
    @DisplayName("创建草稿收件人为空返回统一错误结构")
    void shouldReturnValidationErrorWhenRecipientsEmpty() throws Exception {
        mockMvc.perform(post("/projects/{projectId}/mail-drafts", PROJECT_ID)
                        .header("X-User-Id", ACTOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recipients": [],
                                  "subject": "课程作业提交",
                                  "body": "请查收附件。"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    /**
     * 构造草稿实体用于模拟创建草稿用例返回值。
     *
     * @param recipient 收件人邮箱
     * @return 邮件草稿实体
     */
    private MailDraft createDraft(String recipient) {
        return MailDraft.create(
                PROJECT_ID,
                "package-001",
                List.of(recipient),
                "课程作业提交",
                "请查收附件。",
                ACTOR_ID
        );
    }

    /**
     * 构造已发送草稿实体用于模拟发送草稿用例返回值。
     *
     * @return 已标记发送成功的邮件草稿实体
     */
    private MailDraft sentDraft() {
        MailDraft draft = createDraft("teacher@example.com");
        draft.markSent(CREATED_AT);
        return draft;
    }
}
