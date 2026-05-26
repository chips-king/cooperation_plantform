package com.cooperation.web.permission;

import com.cooperation.domain.permission.PermissionCode;
import com.cooperation.domain.permission.RoleTemplate;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Set;

/**
 * 权限接口 DTO 集合，定义项目权限查询和成员权限更新的 JSON 字段契约。
 */
public final class PermissionDto {

    private PermissionDto() {
    }

    /**
     * 项目权限响应。
     *
     * @param projectId 项目标识。
     * @param members 成员权限列表。
     */
    public record ProjectPermissionResponse(Long projectId, List<MemberPermissionResponse> members) {
    }

    /**
     * 成员权限响应。
     *
     * @param membershipId 成员关系标识。
     * @param userId 用户标识。
     * @param userName 用户展示名称。
     * @param roleTemplate 角色模板。
     * @param permissions 权限点集合。
     */
    public record MemberPermissionResponse(
            Long membershipId,
            Long userId,
            String userName,
            RoleTemplate roleTemplate,
            Set<PermissionCode> permissions
    ) {
    }

    /**
     * 成员权限更新请求。
     *
     * @param operatorId 操作用户标识。
     * @param permissions 新权限集合。
     */
    public record UpdateMemberPermissionRequest(
            Long operatorId,
            @NotEmpty Set<PermissionCode> permissions
    ) {
    }

    /**
     * 成员权限更新响应。
     *
     * @param membershipId 成员关系标识。
     * @param permissions 更新后的权限集合。
     */
    public record UpdateMemberPermissionResponse(Long membershipId, Set<PermissionCode> permissions) {
    }
}
