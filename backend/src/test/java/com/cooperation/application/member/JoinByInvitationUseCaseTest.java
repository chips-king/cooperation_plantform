package com.cooperation.application.member;

import com.cooperation.application.invitation.Invitation;
import com.cooperation.application.invitation.InvitationRepository;
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
 * 邀请加入用例测试，覆盖直接加入与审核加入两种模式。
 */
class JoinByInvitationUseCaseTest {

    @Test
    void directInvitationCreatesActiveMembershipAndOperationLog() {
        FakeInvitationRepository invitationRepository = new FakeInvitationRepository();
        FakeMembershipRepository membershipRepository = new FakeMembershipRepository();
        FakeJoinRequestRepository joinRequestRepository = new FakeJoinRequestRepository();
        FakeOperationLogRepository operationLogRepository = new FakeOperationLogRepository();
        invitationRepository.save(Invitation.directJoin(71L, 21L, 501L, "DIRECT-CODE", 1001L));
        JoinByInvitationUseCase useCase = new JoinByInvitationUseCase(
                invitationRepository,
                membershipRepository,
                joinRequestRepository,
                operationLogRepository
        );

        JoinByInvitationUseCase.Result result = useCase.join(new JoinByInvitationUseCase.Command(
                1002L,
                "DIRECT-CODE"
        ));

        assertThat(result.status()).isEqualTo(JoinByInvitationUseCase.JoinStatus.JOINED);
        assertThat(membershipRepository.savedMembership.getUserId()).isEqualTo(1002L);
        assertThat(membershipRepository.savedMembership.getGroupId()).isEqualTo(21L);
        assertThat(membershipRepository.savedMembership.getProjectId()).contains(501L);
        assertThat(membershipRepository.savedMembership.getStatus()).isEqualTo(Membership.Status.ACTIVE);
        assertThat(joinRequestRepository.savedRequest).isNull();
        assertThat(operationLogRepository.savedLogs)
                .extracting(OperationLog::getAction)
                .containsExactly(OperationAction.MEMBER_JOINED);
        assertThat(operationLogRepository.savedLogs.get(0).getProjectId()).isEqualTo("501");
        assertThat(operationLogRepository.savedLogs.get(0).getActorId()).isEqualTo("1002");
        assertThat(operationLogRepository.savedLogs.get(0).getTargetId()).isEqualTo("1002");
        assertThat(operationLogRepository.savedLogs.get(0).getMetadata()).containsEntry("invitationCode", "DIRECT-CODE");
    }

    @Test
    void reviewInvitationCreatesPendingRequestButNotActiveMembership() {
        FakeInvitationRepository invitationRepository = new FakeInvitationRepository();
        FakeMembershipRepository membershipRepository = new FakeMembershipRepository();
        FakeJoinRequestRepository joinRequestRepository = new FakeJoinRequestRepository();
        FakeOperationLogRepository operationLogRepository = new FakeOperationLogRepository();
        invitationRepository.save(Invitation.reviewRequired(72L, 21L, 501L, "REVIEW-CODE", 1001L));
        JoinByInvitationUseCase useCase = new JoinByInvitationUseCase(
                invitationRepository,
                membershipRepository,
                joinRequestRepository,
                operationLogRepository
        );

        JoinByInvitationUseCase.Result result = useCase.join(new JoinByInvitationUseCase.Command(
                1003L,
                "REVIEW-CODE"
        ));

        assertThat(result.status()).isEqualTo(JoinByInvitationUseCase.JoinStatus.PENDING_REVIEW);
        assertThat(membershipRepository.savedMembership).isNull();
        assertThat(joinRequestRepository.savedRequest.getUserId()).isEqualTo(1003L);
        assertThat(joinRequestRepository.savedRequest.getInvitationId()).isEqualTo(72L);
        assertThat(joinRequestRepository.savedRequest.getStatus()).isEqualTo(JoinRequest.Status.PENDING);
        assertThat(operationLogRepository.savedLogs).isEmpty();
    }

    /**
     * 邀请仓储假实现，按邀请码返回邀请配置。
     */
    private static final class FakeInvitationRepository implements InvitationRepository {

        private Invitation invitation;

        @Override
        public Invitation save(Invitation invitation) {
            this.invitation = invitation;
            return invitation;
        }

        @Override
        public Optional<Invitation> findValidByCode(String code) {
            return Optional.ofNullable(invitation).filter(saved -> saved.getCode().equals(code));
        }
    }

    /**
     * 成员仓储假实现，用于验证审核模式不会直接保存正式成员。
     */
    private static final class FakeMembershipRepository implements MembershipRepository {

        private Membership savedMembership;

        @Override
        public Membership save(Membership membership) {
            savedMembership = membership.withId(20L);
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
            return Optional.ofNullable(savedMembership)
                    .filter(membership -> membership.getProjectId().map(projectId::equals).orElse(false))
                    .filter(membership -> membership.getUserId().equals(userId));
        }

        @Override
        public void deleteByGroupId(Long groupId) {
        }

        @Override
        public void deleteByProjectId(Long projectId) {
        }
    }

    /**
     * 加入申请仓储假实现，只记录待审核申请。
     */
    private static final class FakeJoinRequestRepository implements JoinRequestRepository {

        private JoinRequest savedRequest;

        @Override
        public JoinRequest save(JoinRequest joinRequest) {
            savedRequest = joinRequest.withId(30L);
            return savedRequest;
        }
    }

    /**
     * 操作记录仓储假实现，直接加入时应写入成员加入记录。
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
