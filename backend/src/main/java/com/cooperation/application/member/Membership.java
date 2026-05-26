package com.cooperation.application.member;

import com.cooperation.domain.permission.PermissionSet;
import com.cooperation.domain.permission.RoleTemplate;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * 成员关系应用层实体，表达用户在小组或项目中的角色、状态与自定义权限。
 */
public final class Membership {

    private final Long id;
    private final Long userId;
    private final Long groupId;
    private final Long projectId;
    private final RoleTemplate roleTemplate;
    private final Status status;
    private final PermissionSet customPermissions;

    private Membership(
            Long id,
            Long userId,
            Long groupId,
            Long projectId,
            RoleTemplate roleTemplate,
            Status status,
            PermissionSet customPermissions
    ) {
        this.id = id;
        this.userId = Objects.requireNonNull(userId, "用户标识不能为空");
        this.groupId = Objects.requireNonNull(groupId, "小组标识不能为空");
        this.projectId = projectId;
        this.roleTemplate = Objects.requireNonNull(roleTemplate, "角色模板不能为空");
        this.status = Objects.requireNonNull(status, "成员状态不能为空");
        this.customPermissions = Objects.requireNonNull(customPermissions, "权限集合不能为空");
    }

    /**
     * 创建小组级成员关系。
     *
     * @param userId 用户标识。
     * @param groupId 小组标识。
     * @param roleTemplate 角色模板。
     * @return 小组级成员关系。
     */
    public static Membership groupLevel(Long userId, Long groupId, RoleTemplate roleTemplate) {
        return new Membership(null, userId, groupId, null, roleTemplate, Status.ACTIVE, roleTemplate.defaultPermissions());
    }

    /**
     * 创建项目级成员关系。
     *
     * @param userId 用户标识。
     * @param groupId 小组标识。
     * @param projectId 项目标识。
     * @param roleTemplate 角色模板。
     * @return 项目级成员关系。
     */
    public static Membership projectLevel(Long userId, Long groupId, Long projectId, RoleTemplate roleTemplate) {
        return new Membership(null, userId, groupId, projectId, roleTemplate, Status.ACTIVE, roleTemplate.defaultPermissions());
    }

    /**
     * 复制当前成员关系并替换标识。
     *
     * @param id 成员关系标识。
     * @return 带新标识的成员关系。
     */
    public Membership withId(Long id) {
        return new Membership(id, userId, groupId, projectId, roleTemplate, status, customPermissions);
    }

    /**
     * 复制当前成员关系并替换自定义权限集合。
     *
     * @param permissions 新权限集合。
     * @return 带新权限集合的成员关系。
     */
    public Membership withCustomPermissions(PermissionSet permissions) {
        return new Membership(id, userId, groupId, projectId, roleTemplate, status, permissions);
    }

    /**
     * 判断成员是否拥有指定权限。
     *
     * @param permissions 待匹配的权限集合。
     * @return 全部拥有时返回 true。
     */
    public boolean hasAll(Set<?> permissions) {
        return customPermissions.asSet().containsAll(permissions);
    }

    /**
     * 获取成员关系标识。
     *
     * @return 成员关系标识，新建未保存时为空。
     */
    public Long getId() {
        return id;
    }

    /**
     * 获取用户标识。
     *
     * @return 用户标识。
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * 获取小组标识。
     *
     * @return 小组标识。
     */
    public Long getGroupId() {
        return groupId;
    }

    /**
     * 获取项目标识。
     *
     * @return 项目标识，若为小组级关系则为空。
     */
    public Optional<Long> getProjectId() {
        return Optional.ofNullable(projectId);
    }

    /**
     * 获取角色模板。
     *
     * @return 角色模板。
     */
    public RoleTemplate getRoleTemplate() {
        return roleTemplate;
    }

    /**
     * 获取成员状态。
     *
     * @return 成员状态。
     */
    public Status getStatus() {
        return status;
    }

    /**
     * 获取当前自定义权限集合。
     *
     * @return 权限集合。
     */
    public PermissionSet getCustomPermissions() {
        return customPermissions;
    }

    /**
     * 成员状态枚举。
     */
    public enum Status {
        /** 已生效成员关系。 */
        ACTIVE
    }
}
