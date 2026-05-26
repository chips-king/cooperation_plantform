package com.cooperation.domain.permission;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 权限集合合并规则领域测试。
 */
class PermissionSetTest {

    /**
     * 验证自定义拒绝可以覆盖模板中的默认允许权限。
     */
    @Test
    @DisplayName("自定义拒绝覆盖模板默认允许权限")
    void customDenyOverridesTemplateGrant() {
        PermissionSet permissions = PermissionSet.customize(RoleTemplate.MEMBER)
                .deny(PermissionCode.FILE_DOWNLOAD)
                .build();

        assertFalse(permissions.contains(PermissionCode.FILE_DOWNLOAD));
        assertTrue(permissions.contains(PermissionCode.FILE_UPLOAD));
    }

    /**
     * 验证自定义允许可以在模板基础上追加新的权限点。
     */
    @Test
    @DisplayName("自定义允许在模板基础上追加权限")
    void customGrantAddsPermissionBeyondTemplate() {
        PermissionSet permissions = PermissionSet.customize(RoleTemplate.READ_ONLY)
                .grant(PermissionCode.FILE_DOWNLOAD)
                .grant(PermissionCode.LOG_VIEW)
                .build();

        assertTrue(permissions.contains(PermissionCode.FILE_VIEW));
        assertTrue(permissions.contains(PermissionCode.FILE_DOWNLOAD));
        assertTrue(permissions.contains(PermissionCode.LOG_VIEW));
        assertFalse(permissions.contains(PermissionCode.FILE_UPLOAD));
    }

    /**
     * 验证移除成员自定义权限后，权限结果会回退到角色模板默认规则。
     */
    @Test
    @DisplayName("移除自定义权限后回退到模板默认权限")
    void removingCustomPermissionFallsBackToTemplateDefault() {
        PermissionSet permissions = PermissionSet.customize(RoleTemplate.MEMBER)
                .deny(PermissionCode.FILE_DOWNLOAD)
                .grant(PermissionCode.PACKAGE_CREATE)
                .removeCustom(PermissionCode.FILE_DOWNLOAD)
                .removeCustom(PermissionCode.PACKAGE_CREATE)
                .build();

        assertTrue(permissions.contains(PermissionCode.FILE_DOWNLOAD));
        assertFalse(permissions.contains(PermissionCode.PACKAGE_CREATE));
    }

    /**
     * 验证同一权限点的后续自定义规则可以覆盖先前规则，便于保存最新成员配置。
     */
    @Test
    @DisplayName("同一权限点以后续自定义规则为准")
    void laterCustomRuleOverridesEarlierRule() {
        PermissionSet permissions = PermissionSet.customize(RoleTemplate.READ_ONLY)
                .grant(PermissionCode.FILE_DOWNLOAD)
                .deny(PermissionCode.FILE_DOWNLOAD)
                .build();

        assertFalse(permissions.contains(PermissionCode.FILE_DOWNLOAD));
    }
}
