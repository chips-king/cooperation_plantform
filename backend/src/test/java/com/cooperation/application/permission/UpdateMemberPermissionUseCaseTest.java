package com.cooperation.application.permission;

import com.cooperation.application.member.Membership;
import com.cooperation.application.member.MembershipRepository;
import com.cooperation.domain.log.OperationAction;
import com.cooperation.domain.log.OperationLog;
import com.cooperation.domain.log.OperationLogRepository;
import com.cooperation.domain.permission.PermissionCode;
import com.cooperation.domain.permission.PermissionSet;
import com.cooperation.domain.permission.RoleTemplate;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 成员权限更新用例测试，覆盖负责人授权、普通成员拒绝和操作记录。
 */
class UpdateMemberPermissionUseCaseTest {

    @Test
    void ownerUpdatesMemberPermissionAndWritesOperationLog() {
        FakeMembershipRepository membershipRepository = new FakeMembershipRepository();
        FakeOperationLogRepository operationLogRepository = new FakeOperationLogRepository();
        Membership owner = membershipRepository.save(Membership.projectLevel(1001L, 21L, 501L, RoleTemplate.OWNER));
        Membership member = membershipRepository.save(Membership.projectLevel(1002L, 21L, 501L, RoleTemplate.MEMBER));
        UpdateMemberPermissionUseCase useCase = new UpdateMemberPermissionUseCase(
                membershipRepository,
                operationLogRepository
        );

        UpdateMemberPermissionUseCase.Result result = useCase.update(new UpdateMemberPermissionUseCase.Command(
                owner.getUserId(),
                member.getId(),
                EnumSet.of(PermissionCode.FILE_VIEW, PermissionCode.FILE_UPLOAD, PermissionCode.LOG_VIEW)
        ));

        assertThat(result.membershipId()).isEqualTo(member.getId());
        assertThat(result.permissions())
                .containsExactlyInAnyOrder(PermissionCode.FILE_VIEW, PermissionCode.FILE_UPLOAD, PermissionCode.LOG_VIEW);
        assertThat(membershipRepository.findById(member.getId()))
                .get()
                .extracting(Membership::getCustomPermissions)
                .satisfies(permissions -> {
                    PermissionSet permissionSet = (PermissionSet) permissions;
                    assertThat(permissionSet.asSet()).containsExactlyInAnyOrder(
                            PermissionCode.FILE_VIEW,
                            PermissionCode.FILE_UPLOAD,
                            PermissionCode.LOG_VIEW
                    );
                });
        assertThat(operationLogRepository.savedLogs)
                .extracting(OperationLog::getAction)
                .containsExactly(OperationAction.PERMISSION_UPDATED);
        assertThat(operationLogRepository.savedLogs.get(0).getProjectId()).isEqualTo("501");
        assertThat(operationLogRepository.savedLogs.get(0).getActorId()).isEqualTo("1001");
        assertThat(operationLogRepository.savedLogs.get(0).getTargetId()).isEqualTo(String.valueOf(member.getId()));
        assertThat(operationLogRepository.savedLogs.get(0).getMetadata())
                .containsEntry("targetUserId", "1002")
                .containsEntry("permissions", "FILE_VIEW,FILE_UPLOAD,LOG_VIEW");
    }

    @Test
    void normalMemberCannotUpdateOtherMemberPermission() {
        FakeMembershipRepository membershipRepository = new FakeMembershipRepository();
        FakeOperationLogRepository operationLogRepository = new FakeOperationLogRepository();
        Membership operator = membershipRepository.save(Membership.projectLevel(1002L, 21L, 501L, RoleTemplate.MEMBER));
        Membership target = membershipRepository.save(Membership.projectLevel(1003L, 21L, 501L, RoleTemplate.MEMBER));
        UpdateMemberPermissionUseCase useCase = new UpdateMemberPermissionUseCase(
                membershipRepository,
                operationLogRepository
        );

        assertThatThrownBy(() -> useCase.update(new UpdateMemberPermissionUseCase.Command(
                operator.getUserId(),
                target.getId(),
                EnumSet.of(PermissionCode.FILE_VIEW)
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("无权限");
        assertThat(operationLogRepository.savedLogs).isEmpty();
    }

    /**
     * 成员仓储假实现，支持按成员标识和项目用户标识查询。
     */
    private static final class FakeMembershipRepository implements MembershipRepository {

        private final List<Membership> memberships = new ArrayList<>();

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
    }

    /**
     * 操作记录仓储假实现，验证权限变更审计记录。
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
