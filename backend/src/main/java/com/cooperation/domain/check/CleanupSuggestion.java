package com.cooperation.domain.check;

import java.util.List;

/**
 * 一键清理建议结果。
 *
 * @param items 建议清理项列表
 */
public record CleanupSuggestion(List<CleanupItem> items) {

    /**
     * 防御性复制清理建议列表。
     */
    public CleanupSuggestion {
        items = List.copyOf(items);
    }
}
