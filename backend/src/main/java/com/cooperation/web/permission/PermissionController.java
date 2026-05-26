package com.cooperation.web.permission;

import com.cooperation.application.permission.QueryProjectPermissionUseCase;
import com.cooperation.application.permission.UpdateMemberPermissionUseCase;
import com.cooperation.web.common.ApiResponse;
import com.cooperation.web.permission.PermissionDto.ProjectPermissionResponse;
import com.cooperation.web.permission.PermissionDto.UpdateMemberPermissionRequest;
import com.cooperation.web.permission.PermissionDto.UpdateMemberPermissionResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * 权限控制器，负责项目成员权限查询和成员权限更新接口。
 */
@RestController
public class PermissionController {

    private final ObjectProvider<QueryProjectPermissionUseCase> queryProjectPermissionUseCase;
    private final ObjectProvider<UpdateMemberPermissionUseCase> updateMemberPermissionUseCase;

    /**
     * 创建权限控制器实例。
     *
     * @param queryProjectPermissionUseCase 项目权限查询用例。
     * @param updateMemberPermissionUseCase 成员权限更新用例。
     */
    public PermissionController(
            ObjectProvider<QueryProjectPermissionUseCase> queryProjectPermissionUseCase,
            ObjectProvider<UpdateMemberPermissionUseCase> updateMemberPermissionUseCase
    ) {
        this.queryProjectPermissionUseCase = queryProjectPermissionUseCase;
        this.updateMemberPermissionUseCase = updateMemberPermissionUseCase;
    }

    /**
     * 查询项目成员权限列表。
     *
     * @param projectId 项目标识。
     * @param currentUserId 当前用户标识。
     * @return 统一项目权限响应。
     */
    @GetMapping("/projects/{projectId}/permissions")
    public ApiResponse<ProjectPermissionResponse> getProjectPermissions(
            @PathVariable Long projectId,
            @RequestHeader(name = "X-User-Id", required = false) Long currentUserId
    ) {
        ProjectPermissionResponse response = queryProjectPermissionUseCase.getObject()
                .query(new QueryProjectPermissionUseCase.Query(currentUserId, projectId));
        return ApiResponse.success(response);
    }

    /**
     * 更新成员权限。
     *
     * @param membershipId 成员关系标识。
     * @param currentUserId 当前用户标识。
     * @param request 权限更新请求。
     * @return 统一成员权限更新响应。
     */
    @PatchMapping("/memberships/{membershipId}/permissions")
    public ApiResponse<UpdateMemberPermissionResponse> updateMemberPermission(
            @PathVariable Long membershipId,
            @RequestHeader(name = "X-User-Id", required = false) Long currentUserId,
            @Valid @RequestBody UpdateMemberPermissionRequest request
    ) {
        Long operatorId = currentUserId != null ? currentUserId : request.operatorId();
        UpdateMemberPermissionUseCase.Result result = updateMemberPermissionUseCase.getObject()
                .update(new UpdateMemberPermissionUseCase.Command(operatorId, membershipId, request.permissions()));
        return ApiResponse.success(new UpdateMemberPermissionResponse(result.membershipId(), result.permissions()));
    }
}
