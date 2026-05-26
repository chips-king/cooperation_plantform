package com.cooperation.application.log;

import com.cooperation.domain.log.OperationAction;
import com.cooperation.domain.log.OperationLog;
import com.cooperation.domain.log.OperationLogRepository;
import com.cooperation.domain.permission.PermissionCode;
import com.cooperation.domain.permission.RoleTemplate;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 项目操作记录查询用例。
 */
public class QueryOperationLogUseCase {

    private final OperationLogRepository logRepository;
    private final ProjectPermissionRepository permissionRepository;

    /**
     * 创建项目操作记录查询用例。
     *
     * @param logRepository 操作记录仓储
     * @param permissionRepository 项目权限查询仓储
     */
    public QueryOperationLogUseCase(OperationLogRepository logRepository, ProjectPermissionRepository permissionRepository) {
        this.logRepository = Objects.requireNonNull(logRepository, "操作记录仓储不能为空");
        this.permissionRepository = Objects.requireNonNull(permissionRepository, "项目权限仓储不能为空");
    }

    /**
     * 查询项目操作记录。
     *
     * @param query 查询条件
     * @return 操作记录查询结果
     */
    public Result execute(Query query) {
        Objects.requireNonNull(query, "操作记录查询条件不能为空");
        RoleTemplate roleTemplate = permissionRepository.findRoleTemplate(query.userId(), query.projectId());
        if (!roleTemplate.defaultPermissions().contains(PermissionCode.LOG_VIEW)) {
            throw new AccessDeniedException("当前用户不可查看项目操作记录");
        }

        List<LogItem> logs = logRepository.findByProjectId(query.projectId()).stream()
                .filter(log -> query.action().map(value -> value == log.getAction()).orElse(true))
                .filter(log -> query.actorId().map(value -> value.equals(log.getActorId())).orElse(true))
                .filter(log -> query.from().map(value -> !log.getCreatedAt().isBefore(value)).orElse(true))
                .filter(log -> query.to().map(value -> !log.getCreatedAt().isAfter(value)).orElse(true))
                .sorted(Comparator.comparing(OperationLog::getCreatedAt))
                .map(LogItem::from)
                .toList();
        return new Result(logs);
    }

    /**
     * 项目权限查询仓储端口。
     */
    public interface ProjectPermissionRepository {

        /**
         * 查询用户在项目中的角色模板。
         *
         * @param userId 当前用户标识
         * @param projectId 项目标识
         * @return 用户角色模板
         */
        RoleTemplate findRoleTemplate(Long userId, String projectId);
    }

    /**
     * 操作记录查询条件。
     *
     * @param userId 当前用户标识
     * @param projectId 项目标识
     * @param action 操作类型筛选
     * @param actorId 操作人筛选
     * @param from 开始时间筛选
     * @param to 结束时间筛选
     */
    public record Query(
            Long userId,
            String projectId,
            Optional<OperationAction> action,
            Optional<String> actorId,
            Optional<Instant> from,
            Optional<Instant> to
    ) {

        /**
         * 规范化操作记录查询条件。
         */
        public Query {
            Objects.requireNonNull(userId, "用户标识不能为空");
            Objects.requireNonNull(projectId, "项目标识不能为空");
            action = Objects.requireNonNullElse(action, Optional.empty());
            actorId = Objects.requireNonNullElse(actorId, Optional.empty());
            from = Objects.requireNonNullElse(from, Optional.empty());
            to = Objects.requireNonNullElse(to, Optional.empty());
        }

        /**
         * 创建无筛选的操作记录查询条件。
         *
         * @param userId 当前用户标识
         * @param projectId 项目标识
         * @return 操作记录查询条件
         */
        public static Query all(Long userId, String projectId) {
            return new Query(userId, projectId, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        }
    }

    /**
     * 操作记录查询结果。
     *
     * @param logs 操作记录行列表
     */
    public record Result(List<LogItem> logs) {

        /**
         * 规范化操作记录查询结果。
         */
        public Result {
            logs = List.copyOf(Objects.requireNonNull(logs, "操作记录列表不能为空"));
        }
    }

    /**
     * 操作记录查询行模型。
     *
     * @param projectId 项目标识
     * @param actorId 操作人标识
     * @param action 操作类型
     * @param targetType 目标资源类型
     * @param targetId 目标资源标识
     * @param summary 操作摘要
     * @param createdAt 操作时间
     */
    public record LogItem(
            String projectId,
            String actorId,
            OperationAction action,
            String targetType,
            String targetId,
            String summary,
            Instant createdAt
    ) {

        /**
         * 规范化操作记录查询行模型。
         */
        public LogItem {
            Objects.requireNonNull(projectId, "项目标识不能为空");
            Objects.requireNonNull(actorId, "操作人标识不能为空");
            Objects.requireNonNull(action, "操作类型不能为空");
            Objects.requireNonNull(targetType, "目标资源类型不能为空");
            Objects.requireNonNull(targetId, "目标资源标识不能为空");
            Objects.requireNonNull(summary, "操作摘要不能为空");
            Objects.requireNonNull(createdAt, "操作时间不能为空");
        }

        /**
         * 从领域操作记录创建查询行模型。
         *
         * @param log 领域操作记录
         * @return 操作记录查询行模型
         */
        public static LogItem from(OperationLog log) {
            Objects.requireNonNull(log, "操作记录不能为空");
            return new LogItem(
                    log.getProjectId(),
                    log.getActorId(),
                    log.getAction(),
                    log.getTargetType(),
                    log.getTargetId(),
                    log.getSummary(),
                    log.getCreatedAt()
            );
        }
    }
}
