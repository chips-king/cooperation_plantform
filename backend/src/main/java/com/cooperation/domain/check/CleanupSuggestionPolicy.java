package com.cooperation.domain.check;

import java.util.List;

/**
 * 清理建议生成规则。
 */
public final class CleanupSuggestionPolicy {

    private CleanupSuggestionPolicy() {
    }

    /**
     * 创建默认清理建议规则。
     *
     * @return 默认清理建议规则
     */
    public static CleanupSuggestionPolicy defaultPolicy() {
        return new CleanupSuggestionPolicy();
    }

    /**
     * 从检查问题中筛选可一键清理的项目。
     *
     * @param issues 检查问题列表
     * @return 清理建议
     */
    public CleanupSuggestion suggest(List<CheckIssue> issues) {
        List<CleanupItem> items = issues.stream()
                .filter(issue -> issue.cleanupCandidate() || CheckIssue.isCleanupCandidate(issue.type()))
                .map(CleanupItem::fromIssue)
                .toList();
        return new CleanupSuggestion(items);
    }
}
