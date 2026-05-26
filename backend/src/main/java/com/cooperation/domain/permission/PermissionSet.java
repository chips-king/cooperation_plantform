package com.cooperation.domain.permission;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 权限集合值对象，负责表达角色模板权限与成员自定义权限合并后的结果。
 */
public final class PermissionSet {

    private final Set<PermissionCode> permissions;

    private PermissionSet(Set<PermissionCode> permissions) {
        this.permissions = Set.copyOf(permissions);
    }

    /**
     * 根据权限点集合创建不可变权限集合。
     *
     * @param permissions 已允许的权限点集合。
     * @return 不可变权限集合值对象。
     */
    public static PermissionSet of(Set<PermissionCode> permissions) {
        return new PermissionSet(permissions);
    }

    /**
     * 基于角色模板创建自定义权限构建器。
     *
     * @param template 成员所属角色模板。
     * @return 权限自定义构建器。
     */
    public static Builder customize(RoleTemplate template) {
        return new Builder(template);
    }

    /**
     * 判断当前集合是否包含指定权限点。
     *
     * @param code 待判断的权限点。
     * @return 包含时返回 true，否则返回 false。
     */
    public boolean contains(PermissionCode code) {
        return permissions.contains(code);
    }

    /**
     * 返回当前允许权限点的不可变集合视图。
     *
     * @return 不可变权限点集合。
     */
    public Set<PermissionCode> asSet() {
        return permissions;
    }

    /**
     * 权限自定义构建器，使用后续规则覆盖先前规则。
     */
    public static final class Builder {

        private final RoleTemplate template;
        private final Map<PermissionCode, Boolean> customRules = new EnumMap<>(PermissionCode.class);

        private Builder(RoleTemplate template) {
            this.template = template;
        }

        /**
         * 在模板基础上显式允许某个权限点。
         *
         * @param code 需要追加允许的权限点。
         * @return 当前构建器。
         */
        public Builder grant(PermissionCode code) {
            customRules.put(code, true);
            return this;
        }

        /**
         * 在模板基础上显式拒绝某个权限点。
         *
         * @param code 需要拒绝的权限点。
         * @return 当前构建器。
         */
        public Builder deny(PermissionCode code) {
            customRules.put(code, false);
            return this;
        }

        /**
         * 移除某个权限点的自定义规则，使其回退到模板默认规则。
         *
         * @param code 需要移除自定义规则的权限点。
         * @return 当前构建器。
         */
        public Builder removeCustom(PermissionCode code) {
            customRules.remove(code);
            return this;
        }

        /**
         * 构建合并模板权限与自定义权限后的权限集合。
         *
         * @return 合并后的权限集合值对象。
         */
        public PermissionSet build() {
            Set<PermissionCode> merged = EnumSet.copyOf(template.defaultPermissions().asSet());
            for (Map.Entry<PermissionCode, Boolean> rule : customRules.entrySet()) {
                if (Boolean.TRUE.equals(rule.getValue())) {
                    merged.add(rule.getKey());
                } else {
                    merged.remove(rule.getKey());
                }
            }
            return new PermissionSet(merged);
        }
    }
}
