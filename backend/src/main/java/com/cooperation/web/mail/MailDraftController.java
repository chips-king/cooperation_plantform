package com.cooperation.web.mail;

import com.cooperation.application.mail.CreateMailDraftUseCase;
import com.cooperation.application.mail.QueryMailDraftUseCase;
import com.cooperation.application.mail.SendMailDraftUseCase;
import com.cooperation.application.mail.UpdateMailDraftUseCase;
import com.cooperation.web.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * 邮件草稿 Web API。
 */
@RestController
public class MailDraftController {

    private final CreateMailDraftUseCase createMailDraftUseCase;
    private final QueryMailDraftUseCase queryMailDraftUseCase;
    private final UpdateMailDraftUseCase updateMailDraftUseCase;
    private final SendMailDraftUseCase sendMailDraftUseCase;

    /**
     * 创建邮件草稿控制器。
     *
     * @param createMailDraftUseCase 创建邮件草稿用例
     * @param queryMailDraftUseCase 查询邮件草稿用例
     * @param updateMailDraftUseCase 更新邮件草稿用例
     * @param sendMailDraftUseCase 发送邮件草稿用例
     */
    public MailDraftController(
            CreateMailDraftUseCase createMailDraftUseCase,
            QueryMailDraftUseCase queryMailDraftUseCase,
            UpdateMailDraftUseCase updateMailDraftUseCase,
            SendMailDraftUseCase sendMailDraftUseCase
    ) {
        this.createMailDraftUseCase = createMailDraftUseCase;
        this.queryMailDraftUseCase = queryMailDraftUseCase;
        this.updateMailDraftUseCase = updateMailDraftUseCase;
        this.sendMailDraftUseCase = sendMailDraftUseCase;
    }

    /**
     * 基于项目最近压缩包创建邮件草稿。
     *
     * @param projectId 项目标识
     * @param actorId 当前用户标识
     * @param request 创建草稿请求
     * @return 邮件草稿详情
     */
    @PostMapping("/projects/{projectId}/mail-drafts")
    public ApiResponse<MailDraftDto.MailDraftResponse> createMailDraft(
            @PathVariable String projectId,
            @RequestHeader("X-User-Id") String actorId,
            @Valid @RequestBody MailDraftDto.CreateMailDraftRequest request
    ) {
        CreateMailDraftUseCase.Result result = createMailDraftUseCase.handle(
                new CreateMailDraftUseCase.Command(
                        projectId,
                        actorId,
                        request.recipients(),
                        request.subject(),
                        request.body()
                )
        );
        return ApiResponse.success(MailDraftDto.MailDraftResponse.fromCreatedDraft(
                result.draft(),
                result.attachmentFileName()
        ));
    }

    /**
     * 查询邮件草稿详情。
     *
     * @param draftId 草稿标识
     * @param actorId 当前用户标识
     * @return 邮件草稿详情
     */
    @GetMapping("/mail-drafts/{draftId}")
    public ApiResponse<MailDraftDto.MailDraftResponse> queryMailDraft(
            @PathVariable String draftId,
            @RequestHeader("X-User-Id") String actorId
    ) {
        QueryMailDraftUseCase.Result result = queryMailDraftUseCase.query(
                new QueryMailDraftUseCase.Query(draftId, actorId)
        );
        return ApiResponse.success(MailDraftDto.MailDraftResponse.from(result));
    }

    /**
     * 更新邮件草稿。
     *
     * @param draftId 草稿标识
     * @param actorId 当前用户标识
     * @param request 更新草稿请求
     * @return 更新后的邮件草稿详情
     */
    @PatchMapping("/mail-drafts/{draftId}")
    public ApiResponse<MailDraftDto.MailDraftResponse> updateMailDraft(
            @PathVariable String draftId,
            @RequestHeader("X-User-Id") String actorId,
            @Valid @RequestBody MailDraftDto.UpdateMailDraftRequest request
    ) {
        UpdateMailDraftUseCase.Result result = updateMailDraftUseCase.update(
                new UpdateMailDraftUseCase.Command(
                        draftId,
                        actorId,
                        request.recipients(),
                        request.subject(),
                        request.body(),
                        request.packageId()
                )
        );
        return ApiResponse.success(MailDraftDto.MailDraftResponse.from(result));
    }

    /**
     * 确认并发送邮件草稿。
     *
     * @param draftId 草稿标识
     * @param actorId 当前用户标识
     * @param request 发送确认请求
     * @return 发送后的邮件草稿详情和提示
     */
    @PostMapping("/mail-drafts/{draftId}/send")
    public ApiResponse<MailDraftDto.SendMailDraftResponse> sendMailDraft(
            @PathVariable String draftId,
            @RequestHeader("X-User-Id") String actorId,
            @Valid @RequestBody MailDraftDto.SendMailDraftRequest request
    ) {
        if (!request.confirmed()) {
            throw new IllegalStateException("发送邮件前必须确认");
        }
        SendMailDraftUseCase.Result result = sendMailDraftUseCase.handle(
                new SendMailDraftUseCase.Command(draftId, actorId, true)
        );
        return ApiResponse.success(MailDraftDto.SendMailDraftResponse.from(
                draftId,
                result.draft(),
                result.message()
        ));
    }
}
