package com.cooperation.application.member;

import com.cooperation.domain.permission.RoleTemplate;
import com.cooperation.web.member.MemberDto.ApproveJoinRequestResponse;
import com.cooperation.web.member.MemberDto.JoinInvitationResponse;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * 加入申请审核应用用例，提供通过和拒绝审核的最小端口式契约。
 */
@Service
public class ReviewJoinRequestUseCase {

    /**
     * 通过加入申请。
     *
     * @param command 审核通过命令。
     * @return 审核通过结果。
     */
    public ApproveJoinRequestResponse approve(ApproveCommand command) {
        Objects.requireNonNull(command, "审核通过命令不能为空");
        return new ApproveJoinRequestResponse(
                command.requestId(),
                null,
                null,
                null,
                command.roleTemplate(),
                "approved"
        );
    }

    /**
     * 拒绝加入申请。
     *
     * @param command 审核拒绝命令。
     * @return 审核拒绝结果。
     */
    public JoinInvitationResponse reject(RejectCommand command) {
        Objects.requireNonNull(command, "审核拒绝命令不能为空");
        return new JoinInvitationResponse("rejected", command.reason());
    }

    /**
     * 审核通过命令。
     *
     * @param operatorId 操作用户标识。
     * @param requestId 申请标识。
     * @param roleTemplate 生效角色模板。
     */
    public record ApproveCommand(Long operatorId, Long requestId, RoleTemplate roleTemplate) {
    }

    /**
     * 审核拒绝命令。
     *
     * @param operatorId 操作用户标识。
     * @param requestId 申请标识。
     * @param reason 拒绝原因。
     */
    public record RejectCommand(Long operatorId, Long requestId, String reason) {
    }
}
