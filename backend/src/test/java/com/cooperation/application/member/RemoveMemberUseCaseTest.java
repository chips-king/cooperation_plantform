package com.cooperation.application.member;

import com.cooperation.domain.log.OperationAction;
import com.cooperation.domain.log.OperationLog;
import com.cooperation.domain.log.OperationLogRepository;
import com.cooperation.domain.permission.RoleTemplate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 移除项目成员用例测试，覆盖权限校验和负责人保护规则。
 */
class RemoveMemberUseCaseTest {

    @Test
    void ownerCanRemoveProjectMember() {
        FakeMembershipRepository membershipRepository = new FakeMembershipRepository();
        FakeOperationLogRepository operationLogRepository = new FakeOperationLogRepository();
        Membership owner = membershipRepository.save(Membership.projectLevel(1001L, 21L, 501L, RoleTemplate.OWNER));
        Membership member = membershipRepository.save(Membership.projectLevel(1002L, 21L, 501L, RoleTemplate.MEMBER));
        RemoveMemberUseCase useCase = new RemoveMemberUseCase(membershipRepository, operationLogRepository);

        useCase.remove(new RemoveMemberUseCase.Command(owner.getUserId(), member.getId()));

        assertThat(membershipRepository.deletedIds).containsExactly(member.getId());
        assertThat(operationLogRepository.savedLogs)
                .extracting(OperationLog::getAction)
                .containsExactly(OperationAction.MEMBER_REMOVED);
        assertThat(operationLogRepository.savedLogs.get(0).getMetadata())
                .containsEntry("targetUserId", "1002");
    }

    @Test
    void memberWithoutManagePermissionCannotRemoveOtherMember() {
        FakeMembershipRepository membershipRepository = new FakeMembershipRepository();
        FakeOperationLogRepository operationLogRepository = new FakeOperationLogRepository();
        Membership operator = membershipRepository.save(Membership.projectLevel(1002L, 21L, 501L, RoleTemplate.MEMBER));
        Membership target = membershipRepository.save(Membership.projectLevel(1003L, 21L, 501L, RoleTemplate.MEMBER));
        RemoveMemberUseCase useCase = new RemoveMemberUseCase(membershipRepository, operationLogRepository);

        assertThatThrownBy(() -> useCase.remove(new RemoveMemberUseCase.Command(operator.getUserId(), target.getId())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("无权限");
        assertThat(membershipRepository.deletedIds).isEmpty();
        assertThat(operationLogRepository.savedLogs).isEmpty();
    }

    @Test
    void ownerMembershipCannotBeRemoved() {
        FakeMembershipRepository membershipRepository = new FakeMembershipRepository();
        FakeOperationLogRepository operationLogRepository = new FakeOperationLogRepository();
        Membership owner = membershipRepository.save(Membership.projectLevel(1001L, 21L, 501L, RoleTemplate.OWNER));
        RemoveMemberUseCase useCase = new RemoveMemberUseCase(membershipRepository, operationLogRepository);

        assertThatThrownBy(() -> useCase.remove(new RemoveMemberUseCase.Command(owner.getUserId(), owner.getId())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("负责人");
        assertThat(membershipRepository.deletedIds).isEmpty();
        assertThat(operationLogRepository.savedLogs).isEmpty();
    }

    /**
     * 成员仓储假实现，支持移除成员用例所需的查询和删除操作。
     */
    private static final class FakeMembershipRepository implements MembershipRepository {

        private final List<Membership> memberships = new ArrayList<>();
        private final List<Long> deletedIds = new ArrayList<>();

        @Override
        public Membership save(Membership membership) {
            Membership saved = membership.withId(membership.getId() == null ? (long) memberships.size() + 1 : membership.getId());
            memberships.removeIf(existing -> existing.getId().equals(saved.getId()));
            memberships.add(saved);
            return saved;
        }

        @Override
        public Optional<Membership> findById(Long id) {
            return memberships.stream().filter(membership -> membership.getId().equals(id)).findFirst();
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
        public List<Membership> findByProjectId(Long projectId) {
            return memberships.stream()
                    .filter(membership -> membership.getProjectId().map(projectId::equals).orElse(false))
                    .toList();
        }

        @Override
        public void deleteById(Long id) {
            deletedIds.add(id);
            memberships.removeIf(membership -> membership.getId().equals(id));
        }

        @Override
        public void deleteByGroupId(Long groupId) {
        }

        @Override
        public void deleteByProjectId(Long projectId) {
        }
    }

    /**
     * 操作记录仓储假实现，验证移除成员会写入审计记录。
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
