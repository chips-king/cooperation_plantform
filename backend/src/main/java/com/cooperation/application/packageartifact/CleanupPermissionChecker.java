package com.cooperation.application.packageartifact;

import com.cooperation.domain.check.CleanupItem;
import java.util.List;

/**
 * 清理权限检查器，用于在应用清理建议前统一校验权限。
 */
public interface CleanupPermissionChecker {

    /**
     * 校验操作人是否可以清理指定项目内对象。
     *
     * @param projectId 项目标识
     * @param actorId 操作人标识
     * @param items 待清理项目
     */
    void checkCanCleanup(String projectId, String actorId, List<CleanupItem> items);
}
