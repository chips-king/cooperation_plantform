package com.cooperation.application.member;

import com.cooperation.application.invitation.Invitation;
import com.cooperation.application.invitation.InvitationRepository;
import com.cooperation.domain.log.OperationAction;
import com.cooperation.domain.log.OperationLog;
import com.cooperation.domain.log.OperationLogRepository;
import com.cooperation.domain.permission.RoleTemplate;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * 邀请加入应用用例，按邀请模式创建正式成员或待审核申请。
 */
public final class JoinByInvitationUseCase {

    private final InvitationRepository invitationRepository;
    private final MembershipRepository membershipRepository;
    private final JoinRequestRepository joinRequestRepository;
    private final OperationLogRepository operationLogRepository;

    /**
     * 创建邀请加入用例实例。
     *
     * @param invitationRepository 邀请仓储。
     * @param membershipRepository 成员仓储。
     * @param joinRequestRepository 加入申请仓储。
     * @param operationLogRepository 操作记录仓储。
     */
    public JoinByInvitationUseCase(
            InvitationRepository invitationRepository,
            MembershipRepository membershipRepository,
            JoinRequestRepository joinRequestRepository,
            OperationLogRepository operationLogRepository
    ) {
        this.invitationRepository = Objects.requireNonNull(invitationRepository, "邀请仓储不能为空");
        this.membershipRepository = Objects.requireNonNull(membershipRepository, "成员仓储不能为空");
        this.joinRequestRepository = Objects.requireNonNull(joinRequestRepository, "加入申请仓储不能为空");
        this.operationLogRepository = Objects.requireNonNull(operationLogRepository, "操作记录仓储不能为空");
    }

    /**
     * 执行邀请加入。
     *
     * @param command 邀请加入命令。
     * @return 邀请加入结果。
     */
    public Result join(Command command) {
        Objects.requireNonNull(command, "邀请加入命令不能为空");
        Invitation invitation = invitationRepository.findValidByCode(command.code())
                .orElseThrow(() -> new IllegalArgumentException("邀请不存在或已失效"));
        if (invitation.requiresReview()) {
            joinRequestRepository.save(JoinRequest.pending(command.userId(), invitation.getId()));
            return new Result(JoinStatus.PENDING_REVIEW);
        }

        membershipRepository.save(Membership.projectLevel(
                command.userId(),
                invitation.getGroupId(),
                invitation.getProjectId(),
                RoleTemplate.MEMBER
        ));
        operationLogRepository.save(OperationLog.record(
                String.valueOf(invitation.getProjectId()),
                String.valueOf(command.userId()),
                OperationAction.MEMBER_JOINED,
                "member",
                String.valueOf(command.userId()),
                "成员通过邀请加入项目",
                Map.of("invitationCode", invitation.getCode()),
                Instant.now()
        ));
        return new Result(JoinStatus.JOINED);
    }

    /**
     * 邀请加入命令。
     *
     * @param userId 加入用户标识。
     * @param code 邀请码。
     */
    public record Command(Long userId, String code) {
    }

    /**
     * 邀请加入结果。
     *
     * @param status 加入状态。
     */
    public record Result(JoinStatus status) {
    }

    /**
     * 邀请加入状态。
     */
    public enum JoinStatus {
        /** 已直接加入。 */
        JOINED,

        /** 已提交申请，等待负责人审核。 */
        PENDING_REVIEW
    }
}
