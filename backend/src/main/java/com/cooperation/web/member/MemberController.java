package com.cooperation.web.member;

import com.cooperation.application.member.CreateInvitationUseCase;
import com.cooperation.application.member.JoinByInvitationUseCase;
import com.cooperation.application.member.QueryInvitationUseCase;
import com.cooperation.application.member.RemoveMemberUseCase;
import com.cooperation.application.member.ReviewJoinRequestUseCase;
import com.cooperation.web.common.ApiResponse;
import com.cooperation.web.member.MemberDto.ApproveJoinRequest;
import com.cooperation.web.member.MemberDto.ApproveJoinRequestResponse;
import com.cooperation.web.member.MemberDto.CreateInvitationRequest;
import com.cooperation.web.member.MemberDto.CreateInvitationResponse;
import com.cooperation.web.member.MemberDto.InvitationDetailResponse;
import com.cooperation.web.member.MemberDto.JoinInvitationRequest;
import com.cooperation.web.member.MemberDto.JoinInvitationResponse;
import com.cooperation.web.member.MemberDto.RejectJoinRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * 成员邀请控制器，负责邀请创建、详情查询、加入邀请和加入申请审核接口。
 */
@RestController
public class MemberController {

    private final ObjectProvider<CreateInvitationUseCase> createInvitationUseCase;
    private final ObjectProvider<QueryInvitationUseCase> queryInvitationUseCase;
    private final ObjectProvider<JoinByInvitationUseCase> joinByInvitationUseCase;
    private final ObjectProvider<ReviewJoinRequestUseCase> reviewJoinRequestUseCase;
    private final ObjectProvider<RemoveMemberUseCase> removeMemberUseCase;

    /**
     * 创建成员邀请控制器实例。
     *
     * @param createInvitationUseCase 创建邀请用例。
     * @param queryInvitationUseCase 查询邀请用例。
     * @param joinByInvitationUseCase 加入邀请用例。
     * @param reviewJoinRequestUseCase 审核加入申请用例。
     * @param removeMemberUseCase 移除成员用例。
     */
    public MemberController(
            ObjectProvider<CreateInvitationUseCase> createInvitationUseCase,
            ObjectProvider<QueryInvitationUseCase> queryInvitationUseCase,
            ObjectProvider<JoinByInvitationUseCase> joinByInvitationUseCase,
            ObjectProvider<ReviewJoinRequestUseCase> reviewJoinRequestUseCase,
            ObjectProvider<RemoveMemberUseCase> removeMemberUseCase
    ) {
        this.createInvitationUseCase = createInvitationUseCase;
        this.queryInvitationUseCase = queryInvitationUseCase;
        this.joinByInvitationUseCase = joinByInvitationUseCase;
        this.reviewJoinRequestUseCase = reviewJoinRequestUseCase;
        this.removeMemberUseCase = removeMemberUseCase;
    }

    /**
     * 创建小组邀请链接。
     *
     * @param groupId 小组标识。
     * @param currentUserId 当前用户标识。
     * @param request 创建邀请请求。
     * @return 统一创建邀请响应。
     */
    @PostMapping("/groups/{groupId}/invitations")
    public ApiResponse<CreateInvitationResponse> createInvitation(
            @PathVariable Long groupId,
            @RequestHeader(name = "X-User-Id", required = false) Long currentUserId,
            @Valid @RequestBody CreateInvitationRequest request
    ) {
        CreateInvitationResponse response = createInvitationUseCase.getObject().create(new CreateInvitationUseCase.Command(
                currentUserId,
                groupId,
                request.projectId(),
                request.mode(),
                request.roleTemplate()
        ));
        return ApiResponse.success(response);
    }

    /**
     * 查询邀请详情。
     *
     * @param code 邀请码。
     * @return 统一邀请详情响应。
     */
    @GetMapping("/invitations/{code}")
    public ApiResponse<InvitationDetailResponse> getInvitationDetail(@PathVariable String code) {
        return ApiResponse.success(queryInvitationUseCase.getObject().detail(code));
    }

    /**
     * 通过邀请码加入项目。
     *
     * @param code 邀请码。
     * @param currentUserId 当前用户标识。
     * @param request 加入邀请请求。
     * @return 统一加入结果响应。
     */
    @PostMapping("/invitations/{code}/join")
    public ApiResponse<JoinInvitationResponse> joinInvitation(
            @PathVariable String code,
            @RequestHeader(name = "X-User-Id", required = false) Long currentUserId,
            @Valid @RequestBody JoinInvitationRequest request
    ) {
        Long userId = preferHeaderUser(currentUserId, request.userId());
        JoinByInvitationUseCase.Result result = joinByInvitationUseCase.getObject()
                .join(new JoinByInvitationUseCase.Command(userId, code));
        return ApiResponse.success(toJoinInvitationResponse(result));
    }

    /**
     * 审核通过加入申请。
     *
     * @param requestId 申请标识。
     * @param currentUserId 当前用户标识。
     * @param request 审核通过请求。
     * @return 统一审核通过响应。
     */
    @PostMapping("/join-requests/{requestId}/approve")
    public ApiResponse<ApproveJoinRequestResponse> approveJoinRequest(
            @PathVariable Long requestId,
            @RequestHeader(name = "X-User-Id", required = false) Long currentUserId,
            @Valid @RequestBody ApproveJoinRequest request
    ) {
        Long operatorId = preferHeaderUser(currentUserId, request.operatorId());
        ApproveJoinRequestResponse response = reviewJoinRequestUseCase.getObject().approve(
                new ReviewJoinRequestUseCase.ApproveCommand(operatorId, requestId, request.roleTemplate())
        );
        return ApiResponse.success(response);
    }

    /**
     * 审核拒绝加入申请。
     *
     * @param requestId 申请标识。
     * @param currentUserId 当前用户标识。
     * @param request 审核拒绝请求。
     * @return 统一审核拒绝响应。
     */
    @PostMapping("/join-requests/{requestId}/reject")
    public ApiResponse<JoinInvitationResponse> rejectJoinRequest(
            @PathVariable Long requestId,
            @RequestHeader(name = "X-User-Id", required = false) Long currentUserId,
            @Valid @RequestBody RejectJoinRequest request
    ) {
        Long operatorId = preferHeaderUser(currentUserId, request.operatorId());
        JoinInvitationResponse response = reviewJoinRequestUseCase.getObject().reject(
                new ReviewJoinRequestUseCase.RejectCommand(operatorId, requestId, request.reason())
        );
        return ApiResponse.success(response);
    }

    /**
     * 移除项目成员。
     *
     * @param membershipId 成员关系标识。
     * @param currentUserId 当前用户标识。
     * @return 操作结果。
     */
    @DeleteMapping("/memberships/{membershipId}")
    public ApiResponse<Void> removeMember(
            @PathVariable Long membershipId,
            @RequestHeader("X-User-Id") Long currentUserId
    ) {
        removeMemberUseCase.getObject().remove(new RemoveMemberUseCase.Command(currentUserId, membershipId));
        return ApiResponse.successWithoutData();
    }

    private JoinInvitationResponse toJoinInvitationResponse(JoinByInvitationUseCase.Result result) {
        String status = switch (result.status()) {
            case JOINED -> "joined";
            case PENDING_REVIEW -> "pending_review";
        };
        return new JoinInvitationResponse(status, null);
    }

    private Long preferHeaderUser(Long currentUserId, Long requestUserId) {
        return currentUserId != null ? currentUserId : requestUserId;
    }
}
