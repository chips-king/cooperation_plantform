package com.cooperation.application.home;

import com.cooperation.domain.project.ProjectStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 首页最近项目列表用例测试。
 */
class ListRecentProjectsUseCaseTest {

    /**
     * 验证首页只返回当前用户参与的项目，并按最近更新时间倒序排列。
     */
    @Test
    @DisplayName("按当前用户返回最近参与项目")
    void shouldListRecentProjectsForCurrentUser() {
        FakeRecentProjectPort projectPort = new FakeRecentProjectPort(List.of(
                project(1L, 10L, "课设一组", "结题材料", "2026-05-24T08:00:00Z", List.of(100L)),
                project(2L, 10L, "课设一组", "答辩演示", "2026-05-24T10:00:00Z", List.of(100L, 200L)),
                project(3L, 20L, "课设二组", "无关项目", "2026-05-24T11:00:00Z", List.of(200L))
        ));
        ListRecentProjectsUseCase useCase = new ListRecentProjectsUseCase(projectPort);

        ListRecentProjectsUseCase.Result result = useCase.handle(new ListRecentProjectsUseCase.Query(
                100L,
                Optional.empty(),
                10
        ));

        assertEquals(List.of(2L, 1L), result.projects().stream().map(ListRecentProjectsUseCase.ProjectItem::projectId).toList());
        assertEquals("答辩演示", result.projects().get(0).projectName());
    }

    /**
     * 验证小组筛选不会泄漏其他小组或当前用户未参与的项目。
     */
    @Test
    @DisplayName("按小组筛选最近项目并保持数据隔离")
    void shouldFilterRecentProjectsByGroupAndUser() {
        FakeRecentProjectPort projectPort = new FakeRecentProjectPort(List.of(
                project(1L, 10L, "课设一组", "结题材料", "2026-05-24T08:00:00Z", List.of(100L)),
                project(2L, 20L, "课设二组", "答辩演示", "2026-05-24T10:00:00Z", List.of(100L)),
                project(3L, 10L, "课设一组", "他人归档", "2026-05-24T11:00:00Z", List.of(200L))
        ));
        ListRecentProjectsUseCase useCase = new ListRecentProjectsUseCase(projectPort);

        ListRecentProjectsUseCase.Result result = useCase.handle(new ListRecentProjectsUseCase.Query(
                100L,
                Optional.of(10L),
                10
        ));

        assertEquals(List.of(1L), result.projects().stream().map(ListRecentProjectsUseCase.ProjectItem::projectId).toList());
        assertEquals("课设一组", result.projects().get(0).groupName());
    }

    /**
     * 创建最近项目测试行。
     */
    private static ProjectRow project(
            Long projectId,
            Long groupId,
            String groupName,
            String projectName,
            String updatedAt,
            List<Long> participantIds
    ) {
        return new ProjectRow(projectId, groupId, groupName, projectName, ProjectStatus.ACTIVE, Instant.parse(updatedAt), participantIds);
    }

    /**
     * 首页最近项目内存假端口，按用户、小组和数量限制完成筛选。
     */
    private record FakeRecentProjectPort(List<ProjectRow> rows) implements ListRecentProjectsUseCase.RecentProjectPort {

        @Override
        public List<ListRecentProjectsUseCase.ProjectItem> listRecentProjects(Long userId, Optional<Long> groupId, int limit) {
            return rows.stream()
                    .filter(row -> row.participantIds().contains(userId))
                    .filter(row -> groupId.map(value -> value.equals(row.groupId())).orElse(true))
                    .sorted((left, right) -> right.updatedAt().compareTo(left.updatedAt()))
                    .limit(limit)
                    .map(row -> new ListRecentProjectsUseCase.ProjectItem(
                            row.projectId(),
                            row.groupId(),
                            row.groupName(),
                            row.projectName(),
                            row.status(),
                            row.updatedAt()
                    ))
                    .toList();
        }
    }

    /**
     * 最近项目测试行，保存参与人用于模拟可见范围。
     */
    private record ProjectRow(
            Long projectId,
            Long groupId,
            String groupName,
            String projectName,
            ProjectStatus status,
            Instant updatedAt,
            List<Long> participantIds
    ) {
    }
}
