package com.cooperation.domain.check;

import java.util.List;

/**
 * 打包前检查报告。
 *
 * @param issues 检查发现的问题列表
 */
public record CheckReport(List<CheckIssue> issues) {

    /**
     * 防御性复制检查问题列表。
     */
    public CheckReport {
        issues = List.copyOf(issues);
    }

    /**
     * 判断报告中是否存在阻断打包的问题。
     *
     * @return 存在阻断项时返回 true
     */
    public boolean hasBlockingIssue() {
        return issues.stream().anyMatch(CheckIssue::blocksPackaging);
    }
}
