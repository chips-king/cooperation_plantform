package com.cooperation.infrastructure.persistence.file;

import com.cooperation.application.file.DirectoryManagementUseCase;
import com.cooperation.web.file.FileDto.DirectoryTreeResponse.DirectoryNodeResponse;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

/**
 * 基于 MySQL 的目录管理用例实现。
 */
@Component
public class JdbcDirectoryManagementUseCase implements DirectoryManagementUseCase {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 创建 JDBC 目录管理用例。
     *
     * @param jdbcTemplate JDBC 模板
     */
    public JdbcDirectoryManagementUseCase(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "JDBC 模板不能为空");
    }

    /**
     * 创建指定父目录下的子目录。
     *
     * @param command 创建目录命令
     * @return 新目录节点
     */
    @Override
    public DirectoryNodeResponse create(CreateCommand command) {
        long projectId = parseRequiredLong(command.projectId(), "项目标识");
        long actorId = parseRequiredLong(command.actorId(), "操作人标识");
        String name = validateDirectoryName(command.name());

        // 当父目录标识为 "0" 或空时，创建根目录（parent_id 为 NULL）。
        boolean isRoot = command.parentDirectoryId() == null
                || command.parentDirectoryId().isBlank()
                || "0".equals(command.parentDirectoryId());
        Long parentDirectoryId = isRoot ? null : parseRequiredLong(command.parentDirectoryId(), "父目录标识");

        if (parentDirectoryId != null) {
            assertDirectoryBelongsToProject(projectId, parentDirectoryId);
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        try {
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO directories (project_id, parent_id, name, status, created_by)
                        VALUES (?, ?, ?, 'in_progress', ?)
                        """, Statement.RETURN_GENERATED_KEYS);
                statement.setLong(1, projectId);
                if (parentDirectoryId != null) {
                    statement.setLong(2, parentDirectoryId);
                } else {
                    statement.setNull(2, java.sql.Types.BIGINT);
                }
                statement.setString(3, name);
                statement.setLong(4, actorId);
                return statement;
            }, keyHolder);
        } catch (DuplicateKeyException exception) {
            throw new IllegalStateException("同级目录已存在同名分工目录", exception);
        }

        String directoryId = String.valueOf(Objects.requireNonNull(keyHolder.getKey()).longValue());
        String parentIdStr = parentDirectoryId != null ? String.valueOf(parentDirectoryId) : null;
        return new DirectoryNodeResponse(directoryId, parentIdStr, name, "in_progress", List.of(), List.of());
    }

    /**
     * 删除没有子目录和文件记录的非根目录。
     *
     * @param command 删除目录命令
     * @return 父目录定位信息
     */
    @Override
    public DeleteResult deleteEmpty(DeleteCommand command) {
        long projectId = parseRequiredLong(command.projectId(), "项目标识");
        long directoryId = parseRequiredLong(command.directoryId(), "目录标识");
        DirectoryLocation location = findDirectory(projectId, directoryId);
        if (location.parentDirectoryId() == null) {
            throw new IllegalStateException("根目录不能删除");
        }
        if (countChildren(directoryId) > 0) {
            throw new IllegalStateException("目录下还有子目录，不能删除");
        }
        if (countFiles(directoryId) > 0) {
            throw new IllegalStateException("目录下还有文件或回收站记录，不能删除");
        }

        jdbcTemplate.update("DELETE FROM directories WHERE id = ? AND project_id = ?", directoryId, projectId);
        return new DeleteResult(String.valueOf(location.parentDirectoryId()));
    }

    private DirectoryLocation findDirectory(long projectId, long directoryId) {
        return jdbcTemplate.query("""
                SELECT parent_id
                FROM directories
                WHERE project_id = ? AND id = ?
                """, (rs, row) -> new DirectoryLocation(rs.getObject("parent_id", Long.class)), projectId, directoryId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("目录不存在或不属于当前项目"));
    }

    private void assertDirectoryBelongsToProject(long projectId, long directoryId) {
        findDirectory(projectId, directoryId);
    }

    private long countChildren(long directoryId) {
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM directories WHERE parent_id = ?", Long.class, directoryId);
        return total == null ? 0 : total;
    }

    private long countFiles(long directoryId) {
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM file_assets WHERE directory_id = ?", Long.class, directoryId);
        return total == null ? 0 : total;
    }

    private String validateDirectoryName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("目录名称不能为空");
        }
        String name = value.trim();
        if (name.length() > 255) {
            throw new IllegalArgumentException("目录名称不能超过255个字符");
        }
        if (".".equals(name) || "..".equals(name) || name.contains("/") || name.contains("\\") || name.contains("\u0000")) {
            throw new IllegalArgumentException("目录名称包含非法字符");
        }
        return name;
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

    private record DirectoryLocation(Long parentDirectoryId) {
    }
}
