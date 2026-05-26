package com.cooperation.domain.check;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 打包前检查规则领域测试。
 */
class CheckRuleTest {

    /**
     * 验证空目录只生成提醒风险，不阻断后续打包。
     */
    @Test
    void shouldWarnButNotBlockWhenDirectoryIsEmpty() {
        CheckReport report = CheckRule.defaultRules().inspect(ProjectFileTree.of(
                CheckTarget.directory("docs", true),
                CheckTarget.file("src/App.java", 1280)
        ));

        assertThat(report.issues())
                .anySatisfy(issue -> {
                    assertThat(issue.type()).isEqualTo(CheckIssueType.EMPTY_DIRECTORY);
                    assertThat(issue.path()).isEqualTo("docs");
                    assertThat(issue.level()).isEqualTo(CheckIssueLevel.WARNING);
                    assertThat(issue.blocksPackaging()).isFalse();
                });
        assertThat(report.hasBlockingIssue()).isFalse();
    }

    /**
     * 验证已上传的压缩包会被识别为提醒风险，但不会阻断打包。
     */
    @Test
    void shouldWarnButNotBlockWhenArchiveFileExists() {
        CheckReport report = CheckRule.defaultRules().inspect(ProjectFileTree.of(
                CheckTarget.file("release/member-work.zip", 2048),
                CheckTarget.file("src/App.java", 1280)
        ));

        assertOnlyWarnings(report, CheckIssueType.ARCHIVE_FILE, "release/member-work.zip");
    }

    /**
     * 验证项目缺少说明文档时生成提醒风险，不阻断打包。
     */
    @Test
    void shouldWarnButNotBlockWhenReadmeIsMissing() {
        CheckReport report = CheckRule.defaultRules().inspect(ProjectFileTree.of(
                CheckTarget.file("src/App.java", 1280),
                CheckTarget.file("docs/design.md", 900)
        ));

        assertOnlyWarnings(report, CheckIssueType.MISSING_README, "README.md");
    }

    /**
     * 验证缓存目录和缓存文件会被识别为提醒风险，不阻断打包。
     */
    @Test
    void shouldWarnButNotBlockWhenCacheFilesExist() {
        CheckReport report = CheckRule.defaultRules().inspect(ProjectFileTree.of(
                CheckTarget.directory("__pycache__", false),
                CheckTarget.file("__pycache__/service.cpython-312.pyc", 300),
                CheckTarget.file("README.md", 1200)
        ));

        assertOnlyWarnings(report, CheckIssueType.CACHE_FILE, "__pycache__/service.cpython-312.pyc");
    }

    /**
     * 验证日志文件会被识别为提醒风险，不阻断打包。
     */
    @Test
    void shouldWarnButNotBlockWhenLogFilesExist() {
        CheckReport report = CheckRule.defaultRules().inspect(ProjectFileTree.of(
                CheckTarget.file("logs/app.log", 4096),
                CheckTarget.file("README.md", 1200)
        ));

        assertOnlyWarnings(report, CheckIssueType.LOG_FILE, "logs/app.log");
    }

    /**
     * 断言指定路径的检查项只产生提醒，且整份报告没有阻断项。
     *
     * @param report 检查报告。
     * @param type 期望的检查项类型。
     * @param path 期望的风险路径。
     */
    private void assertOnlyWarnings(CheckReport report, CheckIssueType type, String path) {
        List<CheckIssue> issues = report.issues();

        assertThat(issues)
                .anySatisfy(issue -> {
                    assertThat(issue.type()).isEqualTo(type);
                    assertThat(issue.path()).isEqualTo(path);
                    assertThat(issue.level()).isEqualTo(CheckIssueLevel.WARNING);
                    assertThat(issue.blocksPackaging()).isFalse();
                });
        assertThat(report.hasBlockingIssue()).isFalse();
    }
}
