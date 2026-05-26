package com.cooperation.application.group;

import com.cooperation.application.member.Membership;
import com.cooperation.application.member.MembershipRepository;
import com.cooperation.domain.log.OperationAction;
import com.cooperation.domain.log.OperationLog;
import com.cooperation.domain.log.OperationLogRepository;
import com.cooperation.domain.permission.RoleTemplate;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * 创建小组应用用例，负责保存小组、建立负责人成员关系并写入操作记录。
 */
public final class CreateGroupUseCase {

    private final GroupRepository groupRepository;
    private final MembershipRepository membershipRepository;
    private final OperationLogRepository operationLogRepository;

    /**
     * 创建小组用例实例。
     *
     * @param groupRepository 小组仓储。
     * @param membershipRepository 成员关系仓储。
     * @param operationLogRepository 操作记录仓储。
     */
    public CreateGroupUseCase(
            GroupRepository groupRepository,
            MembershipRepository membershipRepository,
            OperationLogRepository operationLogRepository
    ) {
        this.groupRepository = Objects.requireNonNull(groupRepository, "小组仓储不能为空");
        this.membershipRepository = Objects.requireNonNull(membershipRepository, "成员仓储不能为空");
        this.operationLogRepository = Objects.requireNonNull(operationLogRepository, "操作记录仓储不能为空");
    }

    /**
     * 执行创建小组。
     *
     * @param command 创建小组命令。
     * @return 创建结果。
     */
    public Result create(Command command) {
        Objects.requireNonNull(command, "创建小组命令不能为空");
        Group group = groupRepository.save(Group.create(command.ownerId(), command.name()));
        membershipRepository.save(Membership.groupLevel(command.ownerId(), group.getId(), RoleTemplate.OWNER));
        operationLogRepository.save(OperationLog.record(
                String.valueOf(group.getId()),
                String.valueOf(command.ownerId()),
                OperationAction.GROUP_CREATED,
                "group",
                String.valueOf(group.getId()),
                "创建小组：" + group.getName(),
                Map.of("groupName", group.getName()),
                Instant.now()
        ));
        return new Result(group.getId());
    }

    /**
     * 创建小组命令。
     *
     * @param ownerId 负责人用户标识。
     * @param name 小组名称。
     */
    public record Command(Long ownerId, String name) {
    }

    /**
     * 创建小组结果。
     *
     * @param groupId 小组标识。
     */
    public record Result(Long groupId) {
    }
}
