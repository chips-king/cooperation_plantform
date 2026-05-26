package com.cooperation.domain.permission;

import java.util.EnumSet;
import java.util.Set;

/**
 * 角色模板枚举，定义负责人、成员和只读成员的默认权限集合。
 */
public enum RoleTemplate {

    /** 负责人模板，默认拥有项目内全部权限。 */
    OWNER(EnumSet.allOf(PermissionCode.class)),

    /** 成员模板，默认拥有基础协作权限，不包含成员和权限管理能力。 */
    MEMBER(EnumSet.of(
            PermissionCode.PROJECT_VIEW,
            PermissionCode.FILE_VIEW,
            PermissionCode.FILE_UPLOAD,
            PermissionCode.FILE_DOWNLOAD,
            PermissionCode.FILE_MOVE,
            PermissionCode.FILE_RENAME,
            PermissionCode.FILE_DELETE,
            PermissionCode.FILE_RESTORE,
            PermissionCode.DIRECTORY_STATUS_UPDATE,
            PermissionCode.CHECK_RUN,
            PermissionCode.PACKAGE_DOWNLOAD,
            PermissionCode.LOG_VIEW,
            PermissionCode.NOTIFICATION_VIEW
    )),

    /** 只读模板，仅允许查看项目、文件列表、进度和通知。 */
    READ_ONLY(EnumSet.of(
            PermissionCode.PROJECT_VIEW,
            PermissionCode.FILE_VIEW,
            PermissionCode.NOTIFICATION_VIEW
    ));

    private final Set<PermissionCode> defaultCodes;

    RoleTemplate(Set<PermissionCode> defaultCodes) {
        this.defaultCodes = Set.copyOf(defaultCodes);
    }

    /**
     * 创建当前角色模板的默认权限集合。
     *
     * @return 当前模板对应的权限集合值对象。
     */
    public PermissionSet defaultPermissions() {
        return PermissionSet.of(defaultCodes);
    }
}
