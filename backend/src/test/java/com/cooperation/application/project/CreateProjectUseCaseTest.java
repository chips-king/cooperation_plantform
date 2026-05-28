package com.cooperation.application.project;

import com.cooperation.application.member.Membership;
import com.cooperation.application.member.MembershipRepository;
import com.cooperation.domain.log.OperationAction;
import com.cooperation.domain.log.OperationLog;
import com.cooperation.domain.log.OperationLogRepository;
import com.cooperation.domain.permission.RoleTemplate;
import com.cooperation.domain.project.Project;
import com.cooperation.domain.project.ProjectRepository;
import com.cooperation.domain.project.ProjectStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 创建项目用例测试，表达负责人创建项目和权限拒绝期望。
 */
class CreateProjectUseCaseTest {

    @Test
    void ownerCreatesActiveProjectWithUpdatedAtAndOperationLog() {
        FakeProjectRepository projectRepository = new FakeProjectRepository();
        FakeMembershipRepository membershipRepository = new FakeMembershipRepository();
        FakeOperationLogRepository operationLogRepository = new FakeOperationLogRepository();
        Clock fixedClock = Clock.fixed(Instant.parse("2026-05-24T08:00:00Z"), ZoneOffset.UTC);
        membershipRepository.save(Membership.groupLevel(1001L, 21L, RoleTemplate.OWNER));
        CreateProjectUseCase useCase = new CreateProjectUseCase(
                projectRepository,
                membershipRepository,
                operationLogRepository,
                fixedClock
        );

        CreateProjectUseCase.Result result = useCase.create(new CreateProjectUseCase.Command(
                1001L,
                21L,
                "毕业设计项目"
        ));

        assertThat(result.projectId()).isEqualTo(501L);
        assertThat(result.status()).isEqualTo(ProjectStatus.ACTIVE);
        assertThat(result.updatedAt()).isEqualTo(Instant.parse("2026-05-24T08:00:00Z"));
        assertThat(projectRepository.savedProject.getGroupId()).isEqualTo(21L);
        assertThat(projectRepository.savedProject.getOwnerId()).isEqualTo(1001L);
        assertThat(projectRepository.savedProject.getStatus()).isEqualTo(ProjectStatus.ACTIVE);
        assertThat(operationLogRepository.savedLogs).hasSize(1);
        assertThat(operationLogRepository.savedLogs.get(0).getAction().name()).isEqualTo("PROJECT_CREATED");
        assertThat(operationLogRepository.savedLogs.get(0).getProjectId()).isEqualTo(String.valueOf(result.projectId()));
        assertThat(operationLogRepository.savedLogs.get(0).getActorId()).isEqualTo("1001");
        assertThat(operationLogRepository.savedLogs.get(0).getTargetType()).isEqualTo("project");
        assertThat(operationLogRepository.savedLogs.get(0).getTargetId()).isEqualTo(String.valueOf(result.projectId()));
        assertThat(operationLogRepository.savedLogs.get(0).getMetadata()).containsEntry("projectName", "毕业设计项目");
        assertThat(operationLogRepository.savedLogs.get(0).getSummary()).contains("创建项目");
    }

    @Test
    void memberWithoutProjectManagePermissionCannotCreateProject() {
        FakeProjectRepository projectRepository = new FakeProjectRepository();
        FakeMembershipRepository membershipRepository = new FakeMembershipRepository();
        FakeOperationLogRepository operationLogRepository = new FakeOperationLogRepository();
        Clock fixedClock = Clock.fixed(Instant.parse("2026-05-24T08:00:00Z"), ZoneOffset.UTC);
        membershipRepository.save(Membership.groupLevel(1002L, 21L, RoleTemplate.MEMBER));
        CreateProjectUseCase useCase = new CreateProjectUseCase(
                projectRepository,
                membershipRepository,
                operationLogRepository,
                fixedClock
        );

        assertThatThrownBy(() -> useCase.create(new CreateProjectUseCase.Command(
                1002L,
                21L,
                "无权限项目"
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("无权限");
        assertThat(projectRepository.savedProject).isNull();
        assertThat(operationLogRepository.savedLogs).isEmpty();
    }

    /**
     * 项目仓储假实现，为新项目分配固定标识。
     */
    private static final class FakeProjectRepository implements ProjectRepository {

        private Project savedProject;

        @Override
        public Project save(Project project) {
            savedProject = Project.restore(501L, project.getGroupId(), project.getOwnerId(), project.getName(), project.getStatus());
            return savedProject;
        }

        @Override
        public Optional<Project> findById(Long id) {
            return Optional.ofNullable(savedProject).filter(project -> project.getId().equals(id));
        }

        @Override
        public List<Project> findRecentByUserId(Long userId, int limit) {
            return savedProject == null ? List.of() : List.of(savedProject);
        }

        @Override
        public void deleteById(Long id) {
        }

        @Override
        public int countByGroupId(Long groupId) {
            return savedProject == null ? 0 : (savedProject.getGroupId().equals(groupId) ? 1 : 0);
        }
    }

    /**
     * 成员仓储假实现，按小组和用户返回权限身份。
     */
    private static final class FakeMembershipRepository implements MembershipRepository {

        private final List<Membership> memberships = new ArrayList<>();

        @Override
        public Membership save(Membership membership) {
            Membership saved = membership.withId((long) memberships.size() + 1);
            memberships.add(saved);
            return saved;
        }

        @Override
        public Optional<Membership> findByGroupIdAndUserId(Long groupId, Long userId) {
            return memberships.stream()
                    .filter(membership -> membership.getGroupId().equals(groupId))
                    .filter(membership -> membership.getUserId().equals(userId))
                    .findFirst();
        }

        @Override
        public Optional<Membership> findByProjectIdAndUserId(Long projectId, Long userId) {
            return memberships.stream()
                    .filter(membership -> membership.getProjectId().map(projectId::equals).orElse(false))
                    .filter(membership -> membership.getUserId().equals(userId))
                    .findFirst();
        }

        @Override
        public void deleteByGroupId(Long groupId) {
        }

        @Override
        public void deleteByProjectId(Long projectId) {
        }
    }

    /**
     * 操作记录仓储假实现，保留创建项目后的记录。
     */
    private static final class FakeOperationLogRepository implements OperationLogRepository {

        private final List<OperationLog> savedLogs = new ArrayList<>();

        @Override
        public OperationLog save(OperationLog operationLog) {
            savedLogs.add(operationLog);
            return operationLog;
        }

        @Override
        public List<OperationLog> findByProjectId(String projectId) {
            return savedLogs.stream().filter(log -> log.getProjectId().equals(projectId)).toList();
        }

        @Override
        public List<OperationLog> findByProjectIdAndAction(String projectId, OperationAction action) {
            return savedLogs.stream()
                    .filter(log -> log.getProjectId().equals(projectId))
                    .filter(log -> log.getAction() == action)
                    .toList();
        }

        @Override
        public List<OperationLog> findByProjectIdAndActorId(String projectId, String actorId) {
            return savedLogs.stream()
                    .filter(log -> log.getProjectId().equals(projectId))
                    .filter(log -> log.getActorId().equals(actorId))
                    .toList();
        }

        @Override
        public Optional<OperationLog> findById(String id) {
            return Optional.empty();
        }
    }
}
