package com.cooperation.web.member;

import com.cooperation.domain.permission.RoleTemplate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 成员邀请接口 DTO 集合，定义 Web 请求和响应的 JSON 字段契约。
 */
public final class MemberDto {

    private MemberDto() {
    }

    /**
     * 创建邀请请求。
     *
     * @param projectId 项目标识。
     * @param mode 邀请模式。
     * @param roleTemplate 加入后角色模板。
     */
    public record CreateInvitationRequest(
            @NotNull Long projectId,
            @NotBlank String mode,
            @NotNull RoleTemplate roleTemplate
    ) {
    }

    /**
     * 创建邀请响应。
     *
     * @param invitationId 邀请标识。
     * @param groupId 小组标识。
     * @param projectId 项目标识。
     * @param mode 邀请模式。
     * @param code 邀请码。
     * @param invitationUrl 邀请访问地址。
     */
    public record CreateInvitationResponse(
            Long invitationId,
            Long groupId,
            Long projectId,
            String mode,
            String code,
            String invitationUrl
    ) {
    }

    /**
     * 邀请详情响应。
     *
     * @param invitationId 邀请标识。
     * @param groupId 小组标识。
     * @param groupName 小组名称。
     * @param projectId 项目标识。
     * @param projectName 项目名称。
     * @param mode 邀请模式。
     * @param status 邀请状态。
     */
    public record InvitationDetailResponse(
            Long invitationId,
            Long groupId,
            String groupName,
            Long projectId,
            String projectName,
            String mode,
            String status
    ) {
    }

    /**
     * 加入邀请请求。
     *
     * @param userId 加入用户标识。
     */
    public record JoinInvitationRequest(@NotNull Long userId) {
    }

    /**
     * 加入或拒绝邀请响应。
     *
     * @param status 处理状态。
     * @param message 处理说明。
     */
    public record JoinInvitationResponse(String status, String message) {
    }

    /**
     * 审核通过请求。
     *
     * @param operatorId 操作用户标识。
     * @param roleTemplate 生效角色模板。
     */
    public record ApproveJoinRequest(
            Long operatorId,
            @NotNull RoleTemplate roleTemplate
    ) {
    }

    /**
     * 审核通过响应。
     *
     * @param requestId 申请标识。
     * @param userId 申请用户标识。
     * @param groupId 小组标识。
     * @param projectId 项目标识。
     * @param roleTemplate 生效角色模板。
     * @param status 审核状态。
     */
    public record ApproveJoinRequestResponse(
            Long requestId,
            Long userId,
            Long groupId,
            Long projectId,
            RoleTemplate roleTemplate,
            String status
    ) {
    }

    /**
     * 审核拒绝请求。
     *
     * @param operatorId 操作用户标识。
     * @param reason 拒绝原因。
     */
    public record RejectJoinRequest(
            Long operatorId,
            @NotBlank String reason
    ) {
    }
}
