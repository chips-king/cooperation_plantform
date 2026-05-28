package com.cooperation.application.project;

import com.cooperation.application.member.MembershipRepository;
import com.cooperation.application.log.OperationLogWriter;
import com.cooperation.domain.log.OperationAction;
import com.cooperation.domain.log.OperationLog;
import com.cooperation.domain.log.OperationLogRepository;
import com.cooperation.domain.project.Project;
import com.cooperation.domain.project.ProjectRepository;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public final class DeleteProjectUseCase {

    private final ProjectRepository projectRepository;
    private final MembershipRepository membershipRepository;
    private final OperationLogRepository operationLogRepository;

    public DeleteProjectUseCase(
            ProjectRepository projectRepository,
            MembershipRepository membershipRepository,
            OperationLogRepository operationLogRepository
    ) {
        this.projectRepository = Objects.requireNonNull(projectRepository);
        this.membershipRepository = Objects.requireNonNull(membershipRepository);
        this.operationLogRepository = Objects.requireNonNull(operationLogRepository);
    }

    public Result delete(Command command) {
        Objects.requireNonNull(command);

        Project project = projectRepository.findById(command.projectId())
                .orElseThrow(() -> new IllegalStateException("项目不存在"));

        if (!Objects.equals(project.getOwnerId(), command.actorId())) {
            throw new IllegalStateException("只有项目负责人可以删除项目");
        }

        operationLogRepository.save(OperationLog.record(
                String.valueOf(command.projectId()),
                String.valueOf(command.actorId()),
                OperationAction.PROJECT_DELETED,
                "project",
                String.valueOf(command.projectId()),
                "删除项目：" + project.getName(),
                Map.of(),
                Instant.now()
        ));

        membershipRepository.deleteByProjectId(command.projectId());
        projectRepository.deleteById(command.projectId());

        return new Result(command.projectId());
    }

    public record Command(Long projectId, Long actorId) {}
    public record Result(Long deletedProjectId) {}
}
