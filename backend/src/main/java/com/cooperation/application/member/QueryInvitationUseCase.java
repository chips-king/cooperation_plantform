package com.cooperation.application.member;

import com.cooperation.application.group.Group;
import com.cooperation.application.group.GroupRepository;
import com.cooperation.application.invitation.Invitation;
import com.cooperation.application.invitation.InvitationRepository;
import com.cooperation.domain.project.Project;
import com.cooperation.domain.project.ProjectRepository;
import com.cooperation.web.member.MemberDto.InvitationDetailResponse;
import org.springframework.stereotype.Service;

/**
 * 查询邀请应用用例，提供邀请码详情查询的最小端口式契约。
 */
@Service
public class QueryInvitationUseCase {

    private final InvitationRepository invitationRepository;
    private final GroupRepository groupRepository;
    private final ProjectRepository projectRepository;

    public QueryInvitationUseCase(
            InvitationRepository invitationRepository,
            GroupRepository groupRepository,
            ProjectRepository projectRepository
    ) {
        this.invitationRepository = invitationRepository;
        this.groupRepository = groupRepository;
        this.projectRepository = projectRepository;
    }

    /**
     * 查询邀请详情。
     *
     * @param code 邀请码。
     * @return 邀请详情。
     */
    public InvitationDetailResponse detail(String code) {
        Invitation invitation = invitationRepository.findValidByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("邀请不存在或已失效: " + code));

        String groupName = groupRepository.findById(invitation.getGroupId())
                .map(Group::getName)
                .orElse("未知小组");

        String projectName = projectRepository.findById(invitation.getProjectId())
                .map(Project::getName)
                .orElse("未知项目");

        String mode = invitation.requiresReview() ? "review" : "direct";

        return new InvitationDetailResponse(
                invitation.getId(),
                invitation.getGroupId(),
                groupName,
                invitation.getProjectId(),
                projectName,
                mode,
                "valid"
        );
    }
}
