package com.cooperation.infrastructure.persistence.file;

import com.cooperation.application.file.UploadDirectoryResolver;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 基于 MySQL 目录表的上传路径解析器，负责按相对路径创建缺失子目录。
 */
@Component
public class JdbcUploadDirectoryResolver implements UploadDirectoryResolver {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 创建上传目录解析器。
     *
     * @param jdbcTemplate Spring JDBC 模板
     */
    public JdbcUploadDirectoryResolver(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "JDBC 模板不能为空");
    }

    /**
     * 解析文件夹上传的最终目录；普通文件直接落入当前选中目录。
     *
     * @param projectId 项目标识
     * @param baseDirectoryId 当前选中目录标识
     * @param relativePath 文件夹相对路径
     * @param actorId 当前上传用户标识
     * @return 最终目录标识
     */
    @Override
    public String resolveTargetDirectory(String projectId, String baseDirectoryId, String relativePath, String actorId) {
        long projectKey = parseRequiredLong(projectId, "项目标识");
        long currentDirectoryId = parseRequiredLong(baseDirectoryId, "目录标识");
        long actorKey = parseRequiredLong(actorId, "上传人标识");
        List<String> segments = directorySegments(relativePath);

        for (String segment : segments) {
            currentDirectoryId = findOrCreateDirectory(projectKey, currentDirectoryId, segment, actorKey);
        }

        return String.valueOf(currentDirectoryId);
    }

    private List<String> directorySegments(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return List.of();
        }

        String normalizedPath = relativePath.replace('\\', '/').trim();
        List<String> segments = Arrays.stream(normalizedPath.split("/"))
                .filter(segment -> !segment.isBlank())
                .toList();
        if (segments.size() <= 1) {
            return List.of();
        }

        return segments.subList(0, segments.size() - 1).stream()
                .peek(this::validateSegment)
                .toList();
    }

    private void validateSegment(String segment) {
        if (".".equals(segment) || "..".equals(segment) || segment.contains("\u0000")) {
            throw new IllegalArgumentException("文件夹路径包含非法目录名");
        }
    }

    private long findOrCreateDirectory(long projectId, long parentId, String name, long actorId) {
        List<Long> existingIds = jdbcTemplate.query("""
                SELECT id FROM directories
                WHERE project_id = ? AND parent_id = ? AND name = ?
                LIMIT 1
                """, (rs, row) -> rs.getLong("id"), projectId, parentId, name);
        if (!existingIds.isEmpty()) {
            return existingIds.get(0);
        }

        jdbcTemplate.update("""
                INSERT INTO directories (project_id, parent_id, name, status, created_by)
                VALUES (?, ?, ?, 'in_progress', ?)
                """, projectId, parentId, name, actorId);
        return jdbcTemplate.queryForObject("""
                SELECT id FROM directories
                WHERE project_id = ? AND parent_id = ? AND name = ?
                """, Long.class, projectId, parentId, name);
    }

    private long parseRequiredLong(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(fieldName + "必须是数字", exception);
        }
    }
}
