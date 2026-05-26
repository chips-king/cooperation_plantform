package com.cooperation.application.group;

import com.cooperation.application.member.Membership;
import com.cooperation.application.member.MembershipRepository;
import com.cooperation.domain.log.OperationAction;
import com.cooperation.domain.log.OperationLog;
import com.cooperation.domain.log.OperationLogRepository;
import com.cooperation.domain.permission.RoleTemplate;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 创建小组用例测试，表达负责人建组后的成员关系和操作记录期望。
 */
class CreateGroupUseCaseTest {

    @Test
    void ownerCreatesGroupThenOwnerMembershipAndOperationLogAreCreated() {
        FakeGroupRepository groupRepository = new FakeGroupRepository();
        FakeMembershipRepository membershipRepository = new FakeMembershipRepository();
        FakeOperationLogRepository operationLogRepository = new FakeOperationLogRepository();
        CreateGroupUseCase useCase = new CreateGroupUseCase(
                groupRepository,
                membershipRepository,
                operationLogRepository
        );

        CreateGroupUseCase.Result result = useCase.create(new CreateGroupUseCase.Command(
                1001L,
                "期末协作小组"
        ));

        assertThat(result.groupId()).isEqualTo(1L);
        assertThat(groupRepository.savedGroup.getName()).isEqualTo("期末协作小组");
        assertThat(groupRepository.savedGroup.getOwnerId()).isEqualTo(1001L);
        assertThat(membershipRepository.savedMembership.getUserId()).isEqualTo(1001L);
        assertThat(membershipRepository.savedMembership.getGroupId()).isEqualTo(result.groupId());
        assertThat(membershipRepository.savedMembership.getProjectId()).isEmpty();
        assertThat(membershipRepository.savedMembership.getRoleTemplate()).isEqualTo(RoleTemplate.OWNER);
        assertThat(membershipRepository.savedMembership.getStatus()).isEqualTo(Membership.Status.ACTIVE);
        assertThat(operationLogRepository.savedLogs).hasSize(1);
        assertThat(operationLogRepository.savedLogs.get(0).getAction().name()).isEqualTo("GROUP_CREATED");
        assertThat(operationLogRepository.savedLogs.get(0).getActorId()).isEqualTo("1001");
        assertThat(operationLogRepository.savedLogs.get(0).getTargetType()).isEqualTo("group");
        assertThat(operationLogRepository.savedLogs.get(0).getTargetId()).isEqualTo(String.valueOf(result.groupId()));
        assertThat(operationLogRepository.savedLogs.get(0).getMetadata()).containsEntry("groupName", "期末协作小组");
        assertThat(operationLogRepository.savedLogs.get(0).getSummary()).contains("创建小组");
    }

    /**
     * 小组仓储假实现，只记录本次保存的小组。
     */
    private static final class FakeGroupRepository implements GroupRepository {

        private Group savedGroup;

        @Override
        public Group save(Group group) {
            savedGroup = Group.restore(1L, group.getOwnerId(), group.getName(), group.getStatus());
            return savedGroup;
        }

        @Override
        public Optional<Group> findById(Long id) {
            return Optional.ofNullable(savedGroup).filter(group -> group.getId().equals(id));
        }
    }

    /**
     * 成员仓储假实现，只记录负责人自动加入关系。
     */
    private static final class FakeMembershipRepository implements MembershipRepository {

        private Membership savedMembership;

        @Override
        public Membership save(Membership membership) {
            savedMembership = membership.withId(10L);
            return savedMembership;
        }

        @Override
        public Optional<Membership> findByGroupIdAndUserId(Long groupId, Long userId) {
            return Optional.ofNullable(savedMembership)
                    .filter(membership -> membership.getGroupId().equals(groupId))
                    .filter(membership -> membership.getUserId().equals(userId));
        }

        @Override
        public Optional<Membership> findByProjectIdAndUserId(Long projectId, Long userId) {
            return Optional.empty();
        }
    }

    /**
     * 操作记录仓储假实现，保留保存顺序便于断言。
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
            return savedLogs.stream()
                    .filter(log -> log.getProjectId().equals(projectId))
                    .toList();
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
