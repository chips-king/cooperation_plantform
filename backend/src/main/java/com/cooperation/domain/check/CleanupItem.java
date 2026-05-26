package com.cooperation.domain.check;

/**
 * 一键清理建议项。
 *
 * @param type 清理来源检查类型
 * @param path 建议清理的项目内路径
 */
public record CleanupItem(CheckIssueType type, String path) {

    /**
     * 根据检查问题创建清理建议项。
     *
     * @param issue 检查问题
     * @return 清理建议项
     */
    public static CleanupItem fromIssue(CheckIssue issue) {
        return new CleanupItem(issue.type(), issue.path());
    }
}
