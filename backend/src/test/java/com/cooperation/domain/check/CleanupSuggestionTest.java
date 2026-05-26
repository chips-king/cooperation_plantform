package com.cooperation.domain.check;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 清理建议规则领域测试。
 */
class CleanupSuggestionTest {

    /**
     * 验证明确的缓存、系统遗留、临时和日志文件会进入一键清理建议。
     */
    @Test
    void shouldSuggestCleanupForKnownCacheAndTemporaryFiles() {
        List<CheckIssue> issues = List.of(
                CheckIssue.warning(CheckIssueType.CACHE_FILE, "__pycache__/service.cpython-312.pyc"),
                CheckIssue.warning(CheckIssueType.SYSTEM_JUNK_FILE, ".DS_Store"),
                CheckIssue.warning(CheckIssueType.SYSTEM_JUNK_FILE, "Thumbs.db"),
                CheckIssue.warning(CheckIssueType.TEMPORARY_FILE, "notes.tmp"),
                CheckIssue.warning(CheckIssueType.TEMPORARY_FILE, "old-plan.bak"),
                CheckIssue.warning(CheckIssueType.LOG_FILE, "logs/app.log")
        );

        CleanupSuggestion suggestion = CleanupSuggestionPolicy.defaultPolicy().suggest(issues);

        assertThat(suggestion.items())
                .extracting(CleanupItem::path)
                .containsExactlyInAnyOrder(
                        "__pycache__/service.cpython-312.pyc",
                        ".DS_Store",
                        "Thumbs.db",
                        "notes.tmp",
                        "old-plan.bak",
                        "logs/app.log"
                );
    }

    /**
     * 验证压缩包、空目录和无关大文件只保留为风险提醒，不进入一键清理建议。
     */
    @Test
    void shouldNotSuggestCleanupForArchivesEmptyDirectoriesAndLargeFiles() {
        List<CheckIssue> issues = List.of(
                CheckIssue.warning(CheckIssueType.ARCHIVE_FILE, "release/final.zip"),
                CheckIssue.warning(CheckIssueType.EMPTY_DIRECTORY, "empty-docs"),
                CheckIssue.warning(CheckIssueType.UNRELATED_LARGE_FILE, "videos/demo.mp4")
        );

        CleanupSuggestion suggestion = CleanupSuggestionPolicy.defaultPolicy().suggest(issues);

        assertThat(suggestion.items()).isEmpty();
    }
}
