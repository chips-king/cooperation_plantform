package com.cooperation.application.permission;

import com.cooperation.application.member.Membership;
import com.cooperation.application.member.MembershipRepository;
import com.cooperation.domain.log.OperationAction;
import com.cooperation.domain.log.OperationLog;
import com.cooperation.domain.log.OperationLogRepository;
import com.cooperation.domain.permission.PermissionCode;
import com.cooperation.domain.permission.PermissionSet;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 成员权限更新应用用例，负责校验负责人权限、保存新权限并写入审计记录。
 */
public final class UpdateMemberPermissionUseCase {

    private final MembershipRepository membershipRepository;
    private final OperationLogRepository operationLogRepository;

    /**
     * 创建成员权限更新用例实例。
     *
     * @param membershipRepository 成员仓储。
     * @param operationLogRepository 操作记录仓储。
     */
    public UpdateMemberPermissionUseCase(
            MembershipRepository membershipRepository,
            OperationLogRepository operationLogRepository
    ) {
        this.membershipRepository = Objects.requireNonNull(membershipRepository, "成员仓储不能为空");
        this.operationLogRepository = Objects.requireNonNull(operationLogRepository, "操作记录仓储不能为空");
    }

    /**
     * 执行成员权限更新。
     *
     * @param command 权限更新命令。
     * @return 权限更新结果。
     */
    public Result update(Command command) {
        Objects.requireNonNull(command, "权限更新命令不能为空");
        Membership target = membershipRepository.findById(command.membershipId())
                .orElseThrow(() -> new IllegalArgumentException("成员关系不存在"));
        Long projectId = target.getProjectId().orElseThrow(() -> new IllegalArgumentException("只能更新项目成员权限"));
        Membership operator = membershipRepository.findByProjectIdAndUserId(projectId, command.operatorId())
                .orElseThrow(() -> new IllegalArgumentException("无权限更新成员权限"));
        if (!operator.getCustomPermissions().contains(PermissionCode.PERMISSION_MANAGE)) {
            throw new IllegalArgumentException("无权限更新成员权限");
        }

        PermissionSet permissions = PermissionSet.of(command.permissions());
        Membership saved = membershipRepository.save(target.withCustomPermissions(permissions));
        operationLogRepository.save(OperationLog.record(
                String.valueOf(projectId),
                String.valueOf(command.operatorId()),
                OperationAction.PERMISSION_UPDATED,
                "membership",
                String.valueOf(saved.getId()),
                "更新成员权限",
                Map.of(
                        "targetUserId", String.valueOf(saved.getUserId()),
                        "permissions", permissions.asSet().stream()
                                .sorted(Comparator.comparingInt(Enum::ordinal))
                                .map(PermissionCode::name)
                                .collect(Collectors.joining(","))
                ),
                Instant.now()
        ));
        return new Result(saved.getId(), permissions.asSet());
    }

    /**
     * 权限更新命令。
     *
     * @param operatorId 操作用户标识。
     * @param membershipId 被更新的成员关系标识。
     * @param permissions 新权限集合。
     */
    public record Command(Long operatorId, Long membershipId, Set<PermissionCode> permissions) {
    }

    /**
     * 权限更新结果。
     *
     * @param membershipId 成员关系标识。
     * @param permissions 更新后的权限集合。
     */
    public record Result(Long membershipId, Set<PermissionCode> permissions) {
    }
}
