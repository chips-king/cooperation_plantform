package com.cooperation.application.search;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 全局搜索用例测试。
 */
class SearchUseCaseTest {

    /**
     * 验证项目搜索只返回当前用户可访问且名称匹配的项目。
     */
    @Test
    @DisplayName("按项目名搜索可访问项目")
    void shouldSearchProjectsByNameWithinVisibleScope() {
        FakeSearchRepository repository = new FakeSearchRepository();
        SearchUseCase useCase = new SearchUseCase(repository);

        SearchUseCase.Result result = useCase.execute(new SearchUseCase.Query(100L, "课设"));

        assertEquals(List.of("课设文档"), result.projects().stream().map(SearchUseCase.ProjectHit::projectName).toList());
    }

    /**
     * 验证文件搜索按文件名匹配并过滤无权访问的项目文件。
     */
    @Test
    @DisplayName("按文件名搜索可访问文件")
    void shouldSearchFilesByNameWithinVisibleScope() {
        FakeSearchRepository repository = new FakeSearchRepository();
        SearchUseCase useCase = new SearchUseCase(repository);

        SearchUseCase.Result result = useCase.execute(new SearchUseCase.Query(100L, "报告"));

        assertEquals(List.of("结题报告.docx"), result.files().stream().map(SearchUseCase.FileHit::fileName).toList());
        assertEquals(1L, result.files().get(0).projectId());
    }

    /**
     * 验证成员搜索按成员名匹配并限制在当前用户共同项目范围内。
     */
    @Test
    @DisplayName("按成员名搜索共同项目成员")
    void shouldSearchMembersByNameWithinVisibleScope() {
        FakeSearchRepository repository = new FakeSearchRepository();
        SearchUseCase useCase = new SearchUseCase(repository);

        SearchUseCase.Result result = useCase.execute(new SearchUseCase.Query(100L, "小林"));

        assertEquals(List.of(201L), result.members().stream().map(SearchUseCase.MemberHit::userId).toList());
        assertEquals("小林", result.members().get(0).displayName());
    }

    /**
     * 验证一次搜索会同时返回项目、文件和成员，并统一应用当前用户访问范围。
     */
    @Test
    @DisplayName("一次搜索返回三类可访问结果")
    void shouldSearchProjectsFilesAndMembersTogether() {
        FakeSearchRepository repository = new FakeSearchRepository();
        SearchUseCase useCase = new SearchUseCase(repository);

        SearchUseCase.Result result = useCase.execute(new SearchUseCase.Query(100L, "课设"));

        assertEquals(List.of("课设文档"), result.projects().stream().map(SearchUseCase.ProjectHit::projectName).toList());
        assertEquals(List.of("课设任务说明.md"), result.files().stream().map(SearchUseCase.FileHit::fileName).toList());
        assertEquals(List.of("课设组长"), result.members().stream().map(SearchUseCase.MemberHit::displayName).toList());
    }

    /**
     * 搜索测试内存仓储，表达应用层需要的项目、文件和成员搜索端口。
     */
    private static final class FakeSearchRepository implements SearchUseCase.SearchRepository {

        private final List<ProjectRow> projects = List.of(
                new ProjectRow(1L, 10L, "课设文档", List.of(100L, 201L)),
                new ProjectRow(2L, 20L, "课设私有资料", List.of(200L)),
                new ProjectRow(3L, 10L, "演示材料", List.of(100L))
        );

        private final List<FileRow> files = List.of(
                new FileRow("file-1", 1L, "结题报告.docx", List.of(100L, 201L)),
                new FileRow("file-4", 1L, "课设任务说明.md", List.of(100L, 201L)),
                new FileRow("file-2", 2L, "内部报告.docx", List.of(200L)),
                new FileRow("file-3", 3L, "答辩脚本.md", List.of(100L))
        );

        private final List<MemberRow> members = List.of(
                new MemberRow(201L, "小林", List.of(1L)),
                new MemberRow(204L, "课设组长", List.of(1L)),
                new MemberRow(202L, "小林-外部", List.of(2L)),
                new MemberRow(203L, "小周", List.of(3L))
        );

        @Override
        public List<SearchUseCase.ProjectHit> searchProjects(Long userId, String keyword) {
            return projects.stream()
                    .filter(project -> project.visibleUserIds().contains(userId))
                    .filter(project -> project.name().contains(keyword))
                    .map(project -> new SearchUseCase.ProjectHit(project.id(), project.groupId(), project.name()))
                    .toList();
        }

        @Override
        public List<SearchUseCase.FileHit> searchFiles(Long userId, String keyword) {
            return files.stream()
                    .filter(file -> file.visibleUserIds().contains(userId))
                    .filter(file -> file.name().contains(keyword))
                    .map(file -> new SearchUseCase.FileHit(file.id(), file.projectId(), file.name()))
                    .toList();
        }

        @Override
        public List<SearchUseCase.MemberHit> searchMembers(Long userId, String keyword) {
            List<Long> visibleProjectIds = projects.stream()
                    .filter(project -> project.visibleUserIds().contains(userId))
                    .map(ProjectRow::id)
                    .toList();
            return members.stream()
                    .filter(member -> member.projectIds().stream().anyMatch(visibleProjectIds::contains))
                    .filter(member -> member.displayName().contains(keyword))
                    .map(member -> new SearchUseCase.MemberHit(member.userId(), member.displayName()))
                    .toList();
        }

        private record ProjectRow(Long id, Long groupId, String name, List<Long> visibleUserIds) {
        }

        private record FileRow(String id, Long projectId, String name, List<Long> visibleUserIds) {
        }

        private record MemberRow(Long userId, String displayName, List<Long> projectIds) {
        }
    }
}
