package com.cooperation.application.project;

import com.cooperation.application.member.Membership;
import com.cooperation.application.member.MembershipRepository;
import com.cooperation.domain.log.OperationAction;
import com.cooperation.domain.log.OperationLog;
import com.cooperation.domain.log.OperationLogRepository;
import com.cooperation.domain.permission.PermissionCode;
import com.cooperation.domain.project.Project;
import com.cooperation.domain.project.ProjectRepository;
import com.cooperation.domain.project.ProjectStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * 创建项目应用用例，负责校验小组权限、保存项目并写入操作记录。
 */
public final class CreateProjectUseCase {

    private final ProjectRepository projectRepository;
    private final MembershipRepository membershipRepository;
    private final OperationLogRepository operationLogRepository;
    private final Clock clock;

    /**
     * 创建项目用例实例。
     *
     * @param projectRepository 项目仓储。
     * @param membershipRepository 成员仓储。
     * @param operationLogRepository 操作记录仓储。
     * @param clock 应用时钟。
     */
    public CreateProjectUseCase(
            ProjectRepository projectRepository,
            MembershipRepository membershipRepository,
            OperationLogRepository operationLogRepository,
            Clock clock
    ) {
        this.projectRepository = Objects.requireNonNull(projectRepository, "项目仓储不能为空");
        this.membershipRepository = Objects.requireNonNull(membershipRepository, "成员仓储不能为空");
        this.operationLogRepository = Objects.requireNonNull(operationLogRepository, "操作记录仓储不能为空");
        this.clock = Objects.requireNonNull(clock, "应用时钟不能为空");
    }

    /**
     * 执行创建项目。
     *
     * @param command 创建项目命令。
     * @return 创建项目结果。
     */
    public Result create(Command command) {
        Objects.requireNonNull(command, "创建项目命令不能为空");
        Membership operator = membershipRepository.findByGroupIdAndUserId(command.groupId(), command.ownerId())
                .orElseThrow(() -> new IllegalArgumentException("无权限创建项目"));
        if (!operator.getCustomPermissions().contains(PermissionCode.PROJECT_MANAGE)) {
            throw new IllegalArgumentException("无权限创建项目");
        }

        Project project = projectRepository.save(Project.create(command.groupId(), command.ownerId(), command.name()));
        Instant updatedAt = clock.instant();
        operationLogRepository.save(OperationLog.record(
                String.valueOf(project.getId()),
                String.valueOf(command.ownerId()),
                OperationAction.PROJECT_CREATED,
                "project",
                String.valueOf(project.getId()),
                "创建项目：" + project.getName(),
                Map.of("projectName", project.getName()),
                updatedAt
        ));
        return new Result(project.getId(), project.getStatus(), updatedAt);
    }

    /**
     * 创建项目命令。
     *
     * @param ownerId 操作用户标识。
     * @param groupId 小组标识。
     * @param name 项目名称。
     */
    public record Command(Long ownerId, Long groupId, String name) {
    }

    /**
     * 创建项目结果。
     *
     * @param projectId 项目标识。
     * @param status 项目状态。
     * @param updatedAt 更新时间。
     */
    public record Result(Long projectId, ProjectStatus status, Instant updatedAt) {
    }
}
