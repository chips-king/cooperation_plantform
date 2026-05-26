package com.cooperation.application.home;

import com.cooperation.domain.project.ProjectStatus;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 首页最近项目列表查询用例。
 */
public class ListRecentProjectsUseCase {

    private final RecentProjectPort recentProjectPort;

    /**
     * 创建首页最近项目列表查询用例。
     *
     * @param recentProjectPort 最近项目查询端口
     */
    public ListRecentProjectsUseCase(RecentProjectPort recentProjectPort) {
        this.recentProjectPort = Objects.requireNonNull(recentProjectPort, "最近项目查询端口不能为空");
    }

    /**
     * 查询当前用户最近参与的项目列表。
     *
     * @param query 查询条件
     * @return 最近项目查询结果
     */
    public Result handle(Query query) {
        Objects.requireNonNull(query, "最近项目查询条件不能为空");
        return new Result(recentProjectPort.listRecentProjects(query.userId(), query.groupId(), query.limit()));
    }

    /**
     * 最近项目查询端口。
     */
    public interface RecentProjectPort {

        /**
         * 按用户、小组和数量限制查询最近项目。
         *
         * @param userId 当前用户标识
         * @param groupId 小组筛选条件
         * @param limit 返回数量上限
         * @return 最近项目列表项
         */
        List<ProjectItem> listRecentProjects(Long userId, Optional<Long> groupId, int limit);
    }

    /**
     * 最近项目查询条件。
     *
     * @param userId 当前用户标识
     * @param groupId 小组筛选条件
     * @param limit 返回数量上限
     */
    public record Query(Long userId, Optional<Long> groupId, int limit) {

        /**
         * 校验并规范化最近项目查询条件。
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
     * 最近项目查询结果。
     *
     * @param projects 最近项目列表
     */
    public record Result(List<ProjectItem> projects) {

        /**
         * 规范化最近项目查询结果。
         */
        public Result {
            projects = List.copyOf(Objects.requireNonNull(projects, "最近项目列表不能为空"));
        }
    }

    /**
     * 最近项目列表项。
     *
     * @param projectId 项目标识
     * @param groupId 小组标识
     * @param groupName 小组名称
     * @param projectName 项目名称
     * @param status 项目状态
     * @param updatedAt 最近更新时间
     */
    public record ProjectItem(
            Long projectId,
            Long groupId,
            String groupName,
            String projectName,
            ProjectStatus status,
            Instant updatedAt
    ) {

        /**
         * 校验最近项目列表项。
         */
        public ProjectItem {
            Objects.requireNonNull(projectId, "项目标识不能为空");
            Objects.requireNonNull(groupId, "小组标识不能为空");
            Objects.requireNonNull(groupName, "小组名称不能为空");
            Objects.requireNonNull(projectName, "项目名称不能为空");
            Objects.requireNonNull(status, "项目状态不能为空");
            Objects.requireNonNull(updatedAt, "最近更新时间不能为空");
        }
    }
}
