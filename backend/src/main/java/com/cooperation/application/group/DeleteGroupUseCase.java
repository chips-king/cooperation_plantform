package com.cooperation.application.group;

import com.cooperation.application.member.MembershipRepository;
import com.cooperation.domain.log.OperationAction;
import com.cooperation.domain.log.OperationLog;
import com.cooperation.domain.log.OperationLogRepository;
import com.cooperation.domain.project.ProjectRepository;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * 删除小组应用用例，负责校验负责人身份、检查项目约束并级联清理成员关系。
 */
public final class DeleteGroupUseCase {

    private final GroupRepository groupRepository;
    private final ProjectRepository projectRepository;
    private final MembershipRepository membershipRepository;
    private final OperationLogRepository operationLogRepository;

    public DeleteGroupUseCase(
            GroupRepository groupRepository,
            ProjectRepository projectRepository,
            MembershipRepository membershipRepository,
            OperationLogRepository operationLogRepository
    ) {
        this.groupRepository = Objects.requireNonNull(groupRepository, "小组仓储不能为空");
        this.projectRepository = Objects.requireNonNull(projectRepository, "项目仓储不能为空");
        this.membershipRepository = Objects.requireNonNull(membershipRepository, "成员仓储不能为空");
        this.operationLogRepository = Objects.requireNonNull(operationLogRepository, "操作记录仓储不能为空");
    }

    /**
     * 执行删除小组。
     *
     * @param command 删除小组命令。
     * @return 删除结果。
     */
    public Result delete(Command command) {
        Objects.requireNonNull(command, "删除小组命令不能为空");

        Group group = groupRepository.findById(command.groupId())
                .orElseThrow(() -> new IllegalStateException("小组不存在"));

        if (!Objects.equals(group.getOwnerId(), command.actorId())) {
            throw new IllegalStateException("只有小组负责人可以删除小组");
        }

        int projectCount = projectRepository.countByGroupId(command.groupId());
        if (projectCount > 0) {
            throw new IllegalStateException("小组内还有 " + projectCount + " 个项目，请先删除所有项目再删除小组");
        }

        membershipRepository.deleteByGroupId(command.groupId());
        groupRepository.deleteById(command.groupId());

        operationLogRepository.save(OperationLog.record(
                String.valueOf(command.groupId()),
                String.valueOf(command.actorId()),
                OperationAction.GROUP_CREATED,
                "group",
                String.valueOf(command.groupId()),
                "删除小组：" + group.getName(),
                Map.of("groupName", group.getName()),
                Instant.now()
        ));

        return new Result(command.groupId());
    }

    /**
     * 删除小组命令。
     *
     * @param groupId 小组标识。
     * @param actorId 操作人用户标识。
     */
    public record Command(Long groupId, Long actorId) {
    }

    /**
     * 删除小组结果。
     *
     * @param deletedGroupId 被删除的小组标识。
     */
    public record Result(Long deletedGroupId) {
    }
}
