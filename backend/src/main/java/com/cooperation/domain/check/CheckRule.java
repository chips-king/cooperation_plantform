package com.cooperation.domain.check;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 打包前检查规则。
 */
public final class CheckRule {

    /** 无关大文件提醒阈值，当前按 100MB 识别。 */
    private static final long LARGE_FILE_THRESHOLD_BYTES = 100L * 1024L * 1024L;

    private CheckRule() {
    }

    /**
     * 创建默认检查规则。
     *
     * @return 默认检查规则
     */
    public static CheckRule defaultRules() {
        return new CheckRule();
    }

    /**
     * 检查项目文件树并生成提醒报告。
     *
     * @param tree 项目文件树快照
     * @return 打包前检查报告
     */
    public CheckReport inspect(ProjectFileTree tree) {
        List<CheckIssue> issues = new ArrayList<>();
        boolean hasReadme = false;

        // 逐项扫描目录与文件，所有规则仅生成提醒项，不阻断打包。
        for (CheckTarget target : tree.targets()) {
            if (target.directory()) {
                inspectDirectory(target, issues);
                continue;
            }
            hasReadme = hasReadme || isReadme(target.path());
            inspectFile(target, issues);
        }

        if (!hasReadme) {
            issues.add(CheckIssue.warning(CheckIssueType.MISSING_README, "README.md"));
        }
        return new CheckReport(issues);
    }

    /**
     * 检查目录类风险。
     *
     * @param target 目录检查目标
     * @param issues 当前检查问题列表
     */
    private void inspectDirectory(CheckTarget target, List<CheckIssue> issues) {
        if (target.empty()) {
            issues.add(CheckIssue.warning(CheckIssueType.EMPTY_DIRECTORY, target.path()));
        }
    }

    /**
     * 检查文件类风险。
     *
     * @param target 文件检查目标
     * @param issues 当前检查问题列表
     */
    private void inspectFile(CheckTarget target, List<CheckIssue> issues) {
        String path = target.path();
        String lowerPath = path.toLowerCase(Locale.ROOT);
        if (isArchive(lowerPath)) {
            issues.add(CheckIssue.warning(CheckIssueType.ARCHIVE_FILE, path));
        }
        if (isCache(lowerPath)) {
            issues.add(CheckIssue.warning(CheckIssueType.CACHE_FILE, path));
        }
        if (isSystemJunk(lowerPath)) {
            issues.add(CheckIssue.warning(CheckIssueType.SYSTEM_JUNK_FILE, path));
        }
        if (isTemporary(lowerPath)) {
            issues.add(CheckIssue.warning(CheckIssueType.TEMPORARY_FILE, path));
        }
        if (lowerPath.endsWith(".log")) {
            issues.add(CheckIssue.warning(CheckIssueType.LOG_FILE, path));
        }
        if (target.size() >= LARGE_FILE_THRESHOLD_BYTES) {
            issues.add(CheckIssue.warning(CheckIssueType.UNRELATED_LARGE_FILE, path));
        }
    }

    /**
     * 判断是否为项目说明文档。
     *
     * @param path 项目内路径
     * @return 是说明文档时返回 true
     */
    private boolean isReadme(String path) {
        return "readme.md".equals(path.toLowerCase(Locale.ROOT));
    }

    /**
     * 判断是否为压缩包文件。
     *
     * @param lowerPath 小写项目内路径
     * @return 是压缩包时返回 true
     */
    private boolean isArchive(String lowerPath) {
        return lowerPath.endsWith(".zip")
                || lowerPath.endsWith(".7z")
                || lowerPath.endsWith(".tar.gz")
                || lowerPath.endsWith(".rar");
    }

    /**
     * 判断是否为缓存文件。
     *
     * @param lowerPath 小写项目内路径
     * @return 是缓存文件时返回 true
     */
    private boolean isCache(String lowerPath) {
        return lowerPath.contains("__pycache__/")
                || lowerPath.contains("__pycache__\\")
                || lowerPath.endsWith(".pyc");
    }

    /**
     * 判断是否为系统遗留文件。
     *
     * @param lowerPath 小写项目内路径
     * @return 是系统遗留文件时返回 true
     */
    private boolean isSystemJunk(String lowerPath) {
        return lowerPath.endsWith(".ds_store") || lowerPath.endsWith("thumbs.db");
    }

    /**
     * 判断是否为临时或备份文件。
     *
     * @param lowerPath 小写项目内路径
     * @return 是临时文件时返回 true
     */
    private boolean isTemporary(String lowerPath) {
        return lowerPath.endsWith(".tmp") || lowerPath.endsWith(".bak");
    }
}
