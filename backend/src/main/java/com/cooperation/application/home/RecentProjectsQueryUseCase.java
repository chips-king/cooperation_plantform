package com.cooperation.application.home;

import com.cooperation.domain.project.ProjectStatus;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 首页最近参与项目查询用例。
 */
public class RecentProjectsQueryUseCase {

    private final RecentProjectRepository repository;

    /**
     * 创建首页最近项目查询用例。
     *
     * @param repository 最近项目查询仓储端口
     */
    public RecentProjectsQueryUseCase(RecentProjectRepository repository) {
        this.repository = Objects.requireNonNull(repository, "最近项目查询仓储不能为空");
    }

    /**
     * 查询当前用户最近参与的项目。
     *
     * @param query 查询条件，包含用户、小组筛选和数量限制
     * @return 最近项目列表结果
     */
    public Result execute(Query query) {
        Objects.requireNonNull(query, "最近项目查询条件不能为空");
        return new Result(repository.findRecentProjects(query.userId(), query.groupId(), query.limit()));
    }

    /**
     * 首页最近项目查询仓储端口。
     */
    public interface RecentProjectRepository {

        /**
         * 查询用户可见的最近项目。
         *
         * @param userId 当前用户标识
         * @param groupId 小组筛选条件，为空时不限制小组
         * @param limit 返回数量上限
         * @return 最近项目摘要列表
         */
        List<ProjectSummary> findRecentProjects(Long userId, Optional<Long> groupId, int limit);
    }

    /**
     * 首页最近项目查询条件。
     *
     * @param userId 当前用户标识
     * @param groupId 小组筛选条件
     * @param limit 返回数量上限
     */
    public record Query(Long userId, Optional<Long> groupId, int limit) {

        /**
         * 规范化最近项目查询条件。
         */
        public Query {
            Objects.requireNonNull(userId, "用户标识不能为空");
            groupId = Objects.requireNonNullElse(groupId, Optional.empty());
            if (limit <= 0) {
                throw new IllegalArgumentException("最近项目返回数量必须大于 0");
            }
        }
    }

    /**
     * 首页最近项目查询结果。
     *
     * @param projects 最近项目摘要列表
     */
    public record Result(List<ProjectSummary> projects) {

        /**
         * 规范化最近项目查询结果。
         */
        public Result {
            projects = List.copyOf(Objects.requireNonNull(projects, "最近项目列表不能为空"));
        }
    }

    /**
     * 首页最近项目摘要。
     *
     * @param projectId 项目标识
     * @param groupId 小组标识
     * @param groupName 小组名称
     * @param projectName 项目名称
     * @param status 项目状态
     * @param updatedAt 最近更新时间
     */
    public record ProjectSummary(
            Long projectId,
            Long groupId,
            String groupName,
            String projectName,
            ProjectStatus status,
            Instant updatedAt
    ) {

        /**
         * 规范化最近项目摘要。
         */
        public ProjectSummary {
            Objects.requireNonNull(projectId, "项目标识不能为空");
            Objects.requireNonNull(groupId, "小组标识不能为空");
            Objects.requireNonNull(groupName, "小组名称不能为空");
            Objects.requireNonNull(projectName, "项目名称不能为空");
            Objects.requireNonNull(status, "项目状态不能为空");
            Objects.requireNonNull(updatedAt, "最近更新时间不能为空");
        }
    }
}
