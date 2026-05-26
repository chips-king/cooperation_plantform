package com.cooperation.domain.permission;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 角色模板默认权限领域测试。
 */
class RoleTemplateTest {

    /**
     * 验证只读模板只保留查看类权限，不能下载文件或查看操作记录。
     */
    @Test
    @DisplayName("只读模板不能下载文件且不能查看操作记录")
    void readOnlyTemplateCannotDownloadFiles() {
        PermissionSet permissions = RoleTemplate.READ_ONLY.defaultPermissions();

        assertTrue(permissions.contains(PermissionCode.PROJECT_VIEW));
        assertTrue(permissions.contains(PermissionCode.FILE_VIEW));
        assertFalse(permissions.contains(PermissionCode.FILE_DOWNLOAD));
        assertFalse(permissions.contains(PermissionCode.LOG_VIEW));
        assertFalse(permissions.contains(PermissionCode.FILE_UPLOAD));
    }

    /**
     * 验证成员模板具备基础协作权限，可以下载和维护自己有权限目录内的文件。
     */
    @Test
    @DisplayName("成员模板可以下载文件并执行基础文件协作")
    void memberTemplateCanDownloadFiles() {
        PermissionSet permissions = RoleTemplate.MEMBER.defaultPermissions();

        assertTrue(permissions.contains(PermissionCode.PROJECT_VIEW));
        assertTrue(permissions.contains(PermissionCode.FILE_VIEW));
        assertTrue(permissions.contains(PermissionCode.FILE_UPLOAD));
        assertTrue(permissions.contains(PermissionCode.FILE_DOWNLOAD));
        assertTrue(permissions.contains(PermissionCode.FILE_DELETE));
        assertTrue(permissions.contains(PermissionCode.DIRECTORY_STATUS_UPDATE));
        assertTrue(permissions.contains(PermissionCode.LOG_VIEW));
        assertFalse(permissions.contains(PermissionCode.PERMISSION_MANAGE));
    }

    /**
     * 验证负责人模板拥有项目、成员、权限、文件、打包和项目状态管理权限。
     */
    @Test
    @DisplayName("负责人模板拥有项目内管理权限")
    void ownerTemplateHasManagementPermissions() {
        PermissionSet permissions = RoleTemplate.OWNER.defaultPermissions();

        assertTrue(permissions.contains(PermissionCode.PROJECT_MANAGE));
        assertTrue(permissions.contains(PermissionCode.MEMBER_MANAGE));
        assertTrue(permissions.contains(PermissionCode.PERMISSION_MANAGE));
        assertTrue(permissions.contains(PermissionCode.DIRECTORY_MANAGE));
        assertTrue(permissions.contains(PermissionCode.FILE_DELETE));
        assertTrue(permissions.contains(PermissionCode.CLEANUP_APPLY));
        assertTrue(permissions.contains(PermissionCode.PACKAGE_CREATE));
        assertTrue(permissions.contains(PermissionCode.MAIL_SEND));
        assertTrue(permissions.contains(PermissionCode.PROJECT_END));
        assertTrue(permissions.contains(PermissionCode.PROJECT_REOPEN));
    }
}
