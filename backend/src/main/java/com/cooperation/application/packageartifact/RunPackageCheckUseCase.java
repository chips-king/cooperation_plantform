package com.cooperation.application.packageartifact;

import com.cooperation.domain.check.CheckReport;
import com.cooperation.domain.check.CheckRule;
import com.cooperation.domain.check.CleanupSuggestion;
import com.cooperation.domain.check.CleanupSuggestionPolicy;
import com.cooperation.domain.check.ProjectFileTree;
import com.cooperation.domain.log.OperationAction;
import com.cooperation.domain.log.OperationLog;
import com.cooperation.domain.log.OperationLogRepository;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 运行打包前检查的应用用例。
 */
public class RunPackageCheckUseCase {

    private final ProjectPackageSnapshotRepository snapshots;
    private final OperationLogRepository logs;

    /**
     * 创建打包前检查用例。
     *
     * @param snapshots 项目检查快照仓储
     * @param logs 操作记录仓储
     */
    public RunPackageCheckUseCase(ProjectPackageSnapshotRepository snapshots, OperationLogRepository logs) {
        this.snapshots = Objects.requireNonNull(snapshots, "项目检查快照仓储不能为空");
        this.logs = Objects.requireNonNull(logs, "操作记录仓储不能为空");
    }

    /**
     * 执行打包前检查并生成清理建议。
     *
     * @param command 检查命令
     * @return 检查报告和清理建议
     */
    public Result run(Command command) {
        Objects.requireNonNull(command, "检查命令不能为空");
        ProjectFileTree tree = snapshots.findCheckTreeByProjectId(command.projectId());
        CheckReport report = CheckRule.defaultRules().inspect(tree);
        CleanupSuggestion suggestion = CleanupSuggestionPolicy.defaultPolicy().suggest(report.issues());
        logs.save(OperationLog.record(
                command.projectId(),
                command.actorId(),
                OperationAction.CHECK_RUN,
                "package-check",
                command.projectId(),
                "执行打包前检查",
                Map.of("issueCount", String.valueOf(report.issues().size())),
                Instant.now()
        ));
        return new Result(report, suggestion);
    }

    /**
     * 打包前检查命令。
     *
     * @param projectId 项目标识
     * @param actorId 操作人标识
     */
    public record Command(String projectId, String actorId) {

        /**
         * 校验检查命令必填字段。
         */
        public Command {
            projectId = requireText(projectId, "项目标识不能为空");
            actorId = requireText(actorId, "操作人不能为空");
        }
    }

    /**
     * 打包前检查结果。
     *
     * @param report 检查报告
     * @param cleanupSuggestion 清理建议
     */
    public record Result(CheckReport report, CleanupSuggestion cleanupSuggestion) {

        /**
         * 校验检查结果必填字段。
         */
        public Result {
            Objects.requireNonNull(report, "检查报告不能为空");
            Objects.requireNonNull(cleanupSuggestion, "清理建议不能为空");
        }

        /**
         * 判断当前检查结果是否允许继续打包。
         *
         * @return 没有阻断问题时返回 true
         */
        public boolean canContinuePackaging() {
            return !report.hasBlockingIssue();
        }
    }

    /**
     * 生成操作记录标识，预留给后续持久化实现使用。
     *
     * @return 随机记录标识
     */
    static String newLogId() {
        return UUID.randomUUID().toString();
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
