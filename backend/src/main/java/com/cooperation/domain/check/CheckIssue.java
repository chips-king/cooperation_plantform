package com.cooperation.domain.check;

import java.util.Objects;

/**
 * 打包前检查问题。
 *
 * @param type 检查项类型
 * @param path 问题所在项目内路径
 * @param level 提醒级别
 * @param blocksPackaging 是否阻断后续打包
 * @param cleanupCandidate 是否可进入一键清理建议
 */
public record CheckIssue(
        CheckIssueType type,
        String path,
        CheckIssueLevel level,
        boolean blocksPackaging,
        boolean cleanupCandidate
) {

    /**
     * 创建不阻断打包的提醒项。
     *
     * @param type 检查项类型
     * @param path 问题所在项目内路径
     * @return 提醒级别检查问题
     */
    public static CheckIssue warning(CheckIssueType type, String path) {
        return new CheckIssue(type, requirePath(path), CheckIssueLevel.WARNING, false, isCleanupCandidate(type));
    }

    /**
     * 创建检查问题并校验必要字段。
     */
    public CheckIssue {
        Objects.requireNonNull(type, "检查项类型不能为空");
        path = requirePath(path);
        Objects.requireNonNull(level, "检查项级别不能为空");
    }

    /**
     * 判断检查类型是否属于默认一键清理范围。
     *
     * @param type 检查项类型
     * @return 属于清理范围时返回 true
     */
    public static boolean isCleanupCandidate(CheckIssueType type) {
        return type == CheckIssueType.CACHE_FILE
                || type == CheckIssueType.SYSTEM_JUNK_FILE
                || type == CheckIssueType.TEMPORARY_FILE
                || type == CheckIssueType.LOG_FILE;
    }

    /**
     * 校验项目内路径不能为空。
     *
     * @param path 项目内路径
     * @return 去除首尾空白后的路径
     */
    private static String requirePath(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("检查路径不能为空");
        }
        return path.trim();
    }
}
