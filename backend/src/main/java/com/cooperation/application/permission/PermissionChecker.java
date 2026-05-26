package com.cooperation.application.permission;

import com.cooperation.domain.permission.PermissionCode;

/**
 * 应用层权限检查端口。
 */
public interface PermissionChecker {

    /**
     * 判断用户是否拥有指定项目目录权限。
     *
     * @param userId 用户标识
     * @param projectId 项目标识
     * @param directoryId 目录标识
     * @param permissionCode 权限点
     * @return 拥有权限时返回 true
     */
    boolean hasDirectoryPermission(String userId, String projectId, String directoryId, PermissionCode permissionCode);
}
