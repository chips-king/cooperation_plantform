package com.cooperation.application.member;

import com.cooperation.domain.log.OperationAction;
import com.cooperation.domain.log.OperationLog;
import com.cooperation.domain.log.OperationLogRepository;
import com.cooperation.domain.permission.PermissionCode;
import com.cooperation.domain.permission.RoleTemplate;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * 移除项目成员应用用例，负责校验操作者权限并保护负责人关系不被误删。
 */
@Service
public class RemoveMemberUseCase {

    private final MembershipRepository membershipRepository;
    private final OperationLogRepository operationLogRepository;

    /**
     * 创建移除项目成员用例。
     *
     * @param membershipRepository 成员关系仓储。
     * @param operationLogRepository 操作记录仓储。
     */
    public RemoveMemberUseCase(
            MembershipRepository membershipRepository,
            OperationLogRepository operationLogRepository
    ) {
        this.membershipRepository = Objects.requireNonNull(membershipRepository, "成员关系仓储不能为空");
        this.operationLogRepository = Objects.requireNonNull(operationLogRepository, "操作记录仓储不能为空");
    }

    /**
     * 移除指定项目成员。
     *
     * @param command 移除成员命令。
     */
    public void remove(Command command) {
        Objects.requireNonNull(command, "移除成员命令不能为空");
        Membership target = membershipRepository.findById(command.membershipId())
                .orElseThrow(() -> new IllegalArgumentException("成员关系不存在: " + command.membershipId()));
        Long projectId = target.getProjectId()
                .orElseThrow(() -> new IllegalArgumentException("只能移除项目成员"));
        if (target.getRoleTemplate() == RoleTemplate.OWNER) {
            throw new IllegalArgumentException("不能移除项目负责人");
        }

        Membership operator = membershipRepository.findByProjectIdAndUserId(projectId, command.operatorId())
                .orElseThrow(() -> new IllegalArgumentException("无权限移除项目成员"));
        if (!operator.getCustomPermissions().contains(PermissionCode.MEMBER_MANAGE)) {
            throw new IllegalArgumentException("无权限移除项目成员");
        }

        membershipRepository.deleteById(command.membershipId());
        operationLogRepository.save(OperationLog.record(
                String.valueOf(projectId),
                String.valueOf(command.operatorId()),
                OperationAction.MEMBER_REMOVED,
                "membership",
                String.valueOf(target.getId()),
                "移除项目成员",
                Map.of("targetUserId", String.valueOf(target.getUserId())),
                Instant.now()
        ));
    }

    /**
     * 移除成员命令。
     *
     * @param operatorId 操作用户标识。
     * @param membershipId 被移除的成员关系标识。
     */
    public record Command(Long operatorId, Long membershipId) {
    }
}
