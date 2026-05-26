package com.cooperation.infrastructure.mail;

import com.cooperation.application.mail.MailProviderPort;
import com.cooperation.application.mail.SendMailDraftUseCase;
import com.cooperation.domain.mail.MailDraft;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * 国内邮箱草稿箱 API 占位适配器。
 *
 * <p>第一版只表达 QQ/163 草稿箱 API 的后续接入策略，不读取真实授权、不调用外网。
 * 后续替换时应在本适配器内接入受控的授权配置和 HTTP 客户端，并保持失败信息脱敏。</p>
 */
@Component
public class DomesticMailProviderAdapter implements MailProviderPort, SendMailDraftUseCase.MailProviderPort {

    /**
     * QQ 邮箱草稿策略说明，后续优先适配官方开放能力或企业邮箱网关。
     */
    private static final String QQ_DRAFT_API_STRATEGY = "QQ 邮箱草稿箱 API 策略：预留官方开放能力或企业邮箱网关适配点";

    /**
     * 163 邮箱草稿策略说明，后续优先适配网易邮箱开放能力或企业邮箱网关。
     */
    private static final String NETEASE_DRAFT_API_STRATEGY = "163 邮箱草稿箱 API 策略：预留网易邮箱开放能力或企业邮箱网关适配点";

    /**
     * 对外返回的统一降级错误，避免暴露草稿内容、账号、授权码或第三方响应。
     */
    private static final String DEGRADED_ERROR_MESSAGE =
            "国内邮箱草稿箱 API 尚未启用，请稍后由人工发送或等待邮箱服务配置完成";

    /**
     * 在国内邮箱服务中创建第三方草稿。
     *
     * @param command 创建第三方草稿命令
     * @return 不会返回；当前版本固定降级
     */
    @Override
    public CreateDraftResult createDraft(CreateDraftCommand command) {
        Objects.requireNonNull(command, "创建第三方草稿命令不能为空");
        throw degradedException();
    }

    /**
     * 更新国内邮箱服务中的第三方草稿。
     *
     * @param command 更新第三方草稿命令
     * @return 不会返回；当前版本固定降级
     */
    @Override
    public UpdateDraftResult updateDraft(UpdateDraftCommand command) {
        Objects.requireNonNull(command, "更新第三方草稿命令不能为空");
        throw degradedException();
    }

    /**
     * 发送国内邮箱服务中的第三方草稿。
     *
     * @param command 发送第三方草稿命令
     * @return 不会返回；当前版本固定降级
     */
    @Override
    public SendDraftResult sendDraft(SendDraftCommand command) {
        Objects.requireNonNull(command, "发送第三方草稿命令不能为空");
        throw degradedException();
    }

    /**
     * 兼容发送邮件草稿用例的旧发送端口。
     *
     * @param draftId 草稿标识
     * @param draft 草稿实体
     */
    @Override
    public void sendDraft(String draftId, MailDraft draft) {
        requireText(draftId, "草稿标识不能为空");
        Objects.requireNonNull(draft, "草稿实体不能为空");
        throw degradedException();
    }

    /**
     * 创建统一降级异常。
     *
     * @return 不包含凭据和业务正文的降级异常
     */
    private IllegalStateException degradedException() {
        return new IllegalStateException(DEGRADED_ERROR_MESSAGE + "（已预留：" + strategySummary() + "）");
    }

    /**
     * 汇总当前占位适配器表达的国内邮箱策略。
     *
     * @return 不包含凭据和草稿内容的策略摘要
     */
    private String strategySummary() {
        return QQ_DRAFT_API_STRATEGY + "；" + NETEASE_DRAFT_API_STRATEGY;
    }

    /**
     * 校验必填文本参数。
     *
     * @param value 参数值
     * @param message 参数为空时的错误提示
     * @return 去除首尾空白后的参数值
     */
    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
