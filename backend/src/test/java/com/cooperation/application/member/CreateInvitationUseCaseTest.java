package com.cooperation.application.member;

import com.cooperation.application.invitation.Invitation;
import com.cooperation.application.invitation.InvitationRepository;
import com.cooperation.domain.permission.RoleTemplate;
import com.cooperation.web.member.MemberDto.CreateInvitationResponse;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 创建邀请用例测试，约束前后端使用的邀请模式字段。
 */
class CreateInvitationUseCaseTest {

    @Test
    void reviewModeCreatesReviewRequiredInvitationAndReturnsReviewMode() {
        FakeInvitationRepository invitationRepository = new FakeInvitationRepository();
        CreateInvitationUseCase useCase = new CreateInvitationUseCase(invitationRepository);

        CreateInvitationResponse response = useCase.create(new CreateInvitationUseCase.Command(
                1001L, 21L, 501L, "review", RoleTemplate.MEMBER
        ));

        assertThat(invitationRepository.savedInvitation.requiresReview()).isTrue();
        assertThat(response.mode()).isEqualTo("review");
    }

    @Test
    void directModeCreatesDirectInvitationAndReturnsDirectMode() {
        FakeInvitationRepository invitationRepository = new FakeInvitationRepository();
        CreateInvitationUseCase useCase = new CreateInvitationUseCase(invitationRepository);

        CreateInvitationResponse response = useCase.create(new CreateInvitationUseCase.Command(
                1001L, 21L, 501L, "direct", RoleTemplate.MEMBER
        ));

        assertThat(invitationRepository.savedInvitation.requiresReview()).isFalse();
        assertThat(response.mode()).isEqualTo("direct");
    }

    /**
     * 邀请仓储假实现，记录最后保存的邀请码。
     */
    private static final class FakeInvitationRepository implements InvitationRepository {

        private Invitation savedInvitation;

        @Override
        public Invitation save(Invitation invitation) {
            this.savedInvitation = invitation;
            return invitation;
        }

        @Override
        public Optional<Invitation> findValidByCode(String code) {
            return Optional.ofNullable(savedInvitation)
                    .filter(invitation -> invitation.getCode().equals(code));
        }
    }
}
