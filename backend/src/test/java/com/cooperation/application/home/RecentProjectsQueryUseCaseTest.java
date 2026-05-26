package com.cooperation.application.home;

import com.cooperation.domain.project.Project;
import com.cooperation.domain.project.ProjectStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 首页最近项目查询用例测试。
 */
class RecentProjectsQueryUseCaseTest {

    /**
     * 验证首页按最近更新时间返回当前用户参与的项目。
     */
    @Test
    @DisplayName("按最近更新时间返回当前用户参与项目")
    void shouldListRecentProjectsForCurrentUser() {
        FakeRecentProjectRepository repository = new FakeRecentProjectRepository(List.of(
                recentProject(1L, 10L, "第一组", "课设文档", Instant.parse("2026-05-24T08:00:00Z"), List.of(100L)),
                recentProject(2L, 20L, "第二组", "演示材料", Instant.parse("2026-05-24T09:00:00Z"), List.of(100L)),
                recentProject(3L, 30L, "第三组", "无关项目", Instant.parse("2026-05-24T10:00:00Z"), List.of(200L))
        ));
        RecentProjectsQueryUseCase useCase = new RecentProjectsQueryUseCase(repository);

        RecentProjectsQueryUseCase.Result result = useCase.execute(new RecentProjectsQueryUseCase.Query(100L, Optional.empty(), 10));

        assertEquals(List.of(2L, 1L), result.projects().stream().map(RecentProjectsQueryUseCase.ProjectSummary::projectId).toList());
        assertEquals("演示材料", result.projects().get(0).projectName());
    }

    /**
     * 验证小组筛选只返回目标小组内且当前用户可见的项目。
     */
    @Test
    @DisplayName("支持按小组筛选最近项目")
    void shouldFilterRecentProjectsByGroup() {
        FakeRecentProjectRepository repository = new FakeRecentProjectRepository(List.of(
                recentProject(1L, 10L, "第一组", "课设文档", Instant.parse("2026-05-24T08:00:00Z"), List.of(100L)),
                recentProject(2L, 20L, "第二组", "演示材料", Instant.parse("2026-05-24T09:00:00Z"), List.of(100L)),
                recentProject(3L, 10L, "第一组", "归档资料", Instant.parse("2026-05-24T10:00:00Z"), List.of(200L))
        ));
        RecentProjectsQueryUseCase useCase = new RecentProjectsQueryUseCase(repository);

        RecentProjectsQueryUseCase.Result result = useCase.execute(new RecentProjectsQueryUseCase.Query(100L, Optional.of(10L), 10));

        assertEquals(List.of(1L), result.projects().stream().map(RecentProjectsQueryUseCase.ProjectSummary::projectId).toList());
        assertEquals("第一组", result.projects().get(0).groupName());
    }

    private static FakeRecentProjectRepository.ProjectRow recentProject(
            Long projectId,
            Long groupId,
            String groupName,
            String projectName,
            Instant updatedAt,
            List<Long> visibleUserIds
    ) {
        return new FakeRecentProjectRepository.ProjectRow(
                Project.restore(projectId, groupId, 1L, projectName, ProjectStatus.ACTIVE),
                groupName,
                updatedAt,
                visibleUserIds
        );
    }

    /**
     * 首页查询测试内存仓储，表达应用层需要的最近项目查询端口。
     */
    private record FakeRecentProjectRepository(List<ProjectRow> rows) implements RecentProjectsQueryUseCase.RecentProjectRepository {

        @Override
        public List<RecentProjectsQueryUseCase.ProjectSummary> findRecentProjects(Long userId, Optional<Long> groupId, int limit) {
            return rows.stream()
                    .filter(row -> row.visibleUserIds().contains(userId))
                    .filter(row -> groupId.map(value -> value.equals(row.project().getGroupId())).orElse(true))
                    .sorted((left, right) -> right.updatedAt().compareTo(left.updatedAt()))
                    .limit(limit)
                    .map(row -> new RecentProjectsQueryUseCase.ProjectSummary(
                            row.project().getId(),
                            row.project().getGroupId(),
                            row.groupName(),
                            row.project().getName(),
                            row.project().getStatus(),
                            row.updatedAt()
                    ))
                    .toList();
        }

        private record ProjectRow(Project project, String groupName, Instant updatedAt, List<Long> visibleUserIds) {
        }
    }
}
