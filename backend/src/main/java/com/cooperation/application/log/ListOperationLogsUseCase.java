package com.cooperation.application.log;

import com.cooperation.domain.log.OperationAction;
import com.cooperation.domain.permission.RoleTemplate;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 项目操作记录列表查询用例。
 */
public class ListOperationLogsUseCase {

    private final OperationLogPort operationLogPort;
    private final ProjectAccessPort projectAccessPort;

    /**
     * 创建项目操作记录列表查询用例。
     *
     * @param operationLogPort 操作记录查询端口
     * @param projectAccessPort 项目访问角色查询端口
     */
    public ListOperationLogsUseCase(OperationLogPort operationLogPort, ProjectAccessPort projectAccessPort) {
        this.operationLogPort = Objects.requireNonNull(operationLogPort, "操作记录查询端口不能为空");
        this.projectAccessPort = Objects.requireNonNull(projectAccessPort, "项目访问端口不能为空");
    }

    /**
     * 查询项目操作记录列表。
     *
     * @param query 查询条件
     * @return 操作记录查询结果
     */
    public Result handle(Query query) {
        Objects.requireNonNull(query, "操作记录查询条件不能为空");
        RoleTemplate roleTemplate = projectAccessPort.roleOf(query.userId(), query.projectId());
        if (roleTemplate == RoleTemplate.READ_ONLY) {
            throw new AccessDeniedException("只读用户不可查看操作记录");
        }
        return new Result(operationLogPort.listByProject(query));
    }

    /**
     * 操作记录查询端口。
     */
    public interface OperationLogPort {

        /**
         * 按查询条件列出项目操作记录。
         *
         * @param query 查询条件
         * @return 操作记录列表项
         */
        List<LogItem> listByProject(Query query);
    }

    /**
     * 项目访问角色查询端口。
     */
    public interface ProjectAccessPort {

        /**
         * 查询用户在项目中的角色模板。
         *
         * @param userId 当前用户标识
         * @param projectId 项目标识
         * @return 用户角色模板
         */
        RoleTemplate roleOf(Long userId, String projectId);
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
            Optional<Long> actorId,
            Optional<Instant> from,
            Optional<Instant> to
    ) {

        /**
         * 校验并规范化操作记录查询条件。
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
         * 创建无附加筛选的操作记录查询条件。
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
     * @param logs 操作记录列表
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
     * 操作记录列表项。
     *
     * @param id 操作记录标识
     * @param projectId 项目标识
     * @param actorId 操作人标识
     * @param action 操作类型
     * @param targetType 目标资源类型
     * @param targetId 目标资源标识
     * @param summary 操作摘要
     * @param createdAt 操作时间
     */
    public record LogItem(
            String id,
            String projectId,
            Long actorId,
            OperationAction action,
            String targetType,
            String targetId,
            String summary,
            Instant createdAt
    ) {

        /**
         * 校验操作记录列表项。
         */
        public LogItem {
            Objects.requireNonNull(id, "操作记录标识不能为空");
            Objects.requireNonNull(projectId, "项目标识不能为空");
            Objects.requireNonNull(actorId, "操作人标识不能为空");
            Objects.requireNonNull(action, "操作类型不能为空");
            Objects.requireNonNull(targetType, "目标资源类型不能为空");
            Objects.requireNonNull(targetId, "目标资源标识不能为空");
            Objects.requireNonNull(summary, "操作摘要不能为空");
            Objects.requireNonNull(createdAt, "操作时间不能为空");
        }
    }
}
