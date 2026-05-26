package com.cooperation.application.search;

import java.util.List;
import java.util.Objects;

/**
 * 全局搜索应用用例。
 */
public class SearchUseCase {

    private final SearchRepository repository;

    /**
     * 创建全局搜索用例。
     *
     * @param repository 搜索仓储端口
     */
    public SearchUseCase(SearchRepository repository) {
        this.repository = Objects.requireNonNull(repository, "搜索仓储不能为空");
    }

    /**
     * 按关键字搜索当前用户可访问的项目、文件和成员。
     *
     * @param query 搜索条件
     * @return 搜索结果集合
     */
    public Result execute(Query query) {
        Objects.requireNonNull(query, "搜索条件不能为空");
        String keyword = query.keyword().trim();
        return new Result(
                repository.searchProjects(query.userId(), keyword),
                repository.searchFiles(query.userId(), keyword),
                repository.searchMembers(query.userId(), keyword)
        );
    }

    /**
     * 全局搜索仓储端口。
     */
    public interface SearchRepository {

        /**
         * 搜索当前用户可访问的项目。
         *
         * @param userId 当前用户标识
         * @param keyword 搜索关键字
         * @return 项目命中列表
         */
        List<ProjectHit> searchProjects(Long userId, String keyword);

        /**
         * 搜索当前用户可访问的文件。
         *
         * @param userId 当前用户标识
         * @param keyword 搜索关键字
         * @return 文件命中列表
         */
        List<FileHit> searchFiles(Long userId, String keyword);

        /**
         * 搜索当前用户共同项目范围内的成员。
         *
         * @param userId 当前用户标识
         * @param keyword 搜索关键字
         * @return 成员命中列表
         */
        List<MemberHit> searchMembers(Long userId, String keyword);
    }

    /**
     * 搜索条件。
     *
     * @param userId 当前用户标识
     * @param keyword 搜索关键字
     */
    public record Query(Long userId, String keyword) {

        /**
         * 规范化搜索条件。
         */
        public Query {
            Objects.requireNonNull(userId, "用户标识不能为空");
            keyword = Objects.requireNonNull(keyword, "搜索关键字不能为空");
        }
    }

    /**
     * 搜索结果。
     *
     * @param projects 项目命中列表
     * @param files 文件命中列表
     * @param members 成员命中列表
     */
    public record Result(List<ProjectHit> projects, List<FileHit> files, List<MemberHit> members) {

        /**
         * 规范化搜索结果。
         */
        public Result {
            projects = List.copyOf(Objects.requireNonNull(projects, "项目搜索结果不能为空"));
            files = List.copyOf(Objects.requireNonNull(files, "文件搜索结果不能为空"));
            members = List.copyOf(Objects.requireNonNull(members, "成员搜索结果不能为空"));
        }
    }

    /**
     * 项目搜索命中项。
     *
     * @param projectId 项目标识
     * @param groupId 小组标识
     * @param projectName 项目名称
     */
    public record ProjectHit(Long projectId, Long groupId, String projectName) {

        /**
         * 规范化项目命中项。
         */
        public ProjectHit {
            Objects.requireNonNull(projectId, "项目标识不能为空");
            Objects.requireNonNull(groupId, "小组标识不能为空");
            Objects.requireNonNull(projectName, "项目名称不能为空");
        }
    }

    /**
     * 文件搜索命中项。
     *
     * @param fileId 文件标识
     * @param projectId 项目标识
     * @param fileName 文件名称
     */
    public record FileHit(String fileId, Long projectId, String fileName) {

        /**
         * 规范化文件命中项。
         */
        public FileHit {
            Objects.requireNonNull(fileId, "文件标识不能为空");
            Objects.requireNonNull(projectId, "项目标识不能为空");
            Objects.requireNonNull(fileName, "文件名称不能为空");
        }
    }

    /**
     * 成员搜索命中项。
     *
     * @param userId 用户标识
     * @param displayName 展示名称
     */
    public record MemberHit(Long userId, String displayName) {

        /**
         * 规范化成员命中项。
         */
        public MemberHit {
            Objects.requireNonNull(userId, "用户标识不能为空");
            Objects.requireNonNull(displayName, "展示名称不能为空");
        }
    }
}
