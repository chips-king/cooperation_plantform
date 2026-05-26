package com.cooperation.infrastructure.persistence.project;

import com.cooperation.domain.project.Project;
import com.cooperation.domain.project.ProjectRepository;
import com.cooperation.domain.project.ProjectStatus;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/**
 * 基于 {@code projects} 表的项目仓储实现。
 */
@Repository
public class MyBatisProjectRepository implements ProjectRepository {

    /** 项目基础查询语句，通过所属小组补齐领域所需的负责人标识。 */
    private static final String SELECT_PROJECT = """
            SELECT p.id, p.group_id, ug.owner_id, p.name, p.status
            FROM projects p
            JOIN user_groups ug ON ug.id = p.group_id
            """;

    /** 最近项目默认排序，按数据库维护的更新时间倒序返回。 */
    private static final String RECENT_PROJECT_ORDER = " ORDER BY p.updated_at DESC, p.id DESC LIMIT ?";

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<Project> projectRowMapper = this::mapProject;

    /**
     * 创建项目仓储实现。
     *
     * @param jdbcTemplate Spring JDBC 模板。
     */
    public MyBatisProjectRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "JDBC 模板不能为空");
    }

    /**
     * 保存项目；新项目写入 {@code projects} 表，已有项目更新名称、状态和状态时间。
     *
     * @param project 待保存的项目聚合。
     * @return 保存后的项目聚合。
     */
    @Override
    public Project save(Project project) {
        Objects.requireNonNull(project, "项目不能为空");
        if (project.getId() == null) {
            return insert(project);
        }
        update(project);
        return findById(project.getId()).orElse(project);
    }

    /**
     * 按项目唯一标识查询项目。
     *
     * @param id 项目唯一标识。
     * @return 找到时返回项目聚合，否则返回空。
     */
    @Override
    public Optional<Project> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    SELECT_PROJECT + " WHERE p.id = ?",
                    projectRowMapper,
                    id
            ));
        } catch (EmptyResultDataAccessException ignored) {
            return Optional.empty();
        }
    }

    /**
     * 按负责人用户标识查询最近项目。
     *
     * @param userId 用户唯一标识。
     * @param limit 返回项目数量上限。
     * @return 最近项目列表。
     */
    @Override
    public List<Project> findRecentByUserId(Long userId, int limit) {
        if (userId == null || limit <= 0) {
            return List.of();
        }
        return jdbcTemplate.query(
                SELECT_PROJECT + " WHERE ug.owner_id = ?" + RECENT_PROJECT_ORDER,
                projectRowMapper,
                userId,
                limit
        );
    }

    /**
     * 插入新项目并按生成主键重新查询，确保负责人来自 {@code user_groups.owner_id}。
     *
     * @param project 新项目聚合。
     * @return 插入后的项目聚合。
     */
    private Project insert(Project project) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO projects (group_id, name, status, ended_at, reopened_at)
                    VALUES (?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            fillSaveStatement(statement, project, 1);
            return statement;
        }, keyHolder);

        Number key = Objects.requireNonNull(keyHolder.getKey(), "项目主键生成失败");
        createDefaultDirectory(key.longValue(), project.getOwnerId());
        return findById(key.longValue())
                .orElseThrow(() -> new IllegalStateException("项目保存后无法读取"));
    }

    private void createDefaultDirectory(Long projectId, Long ownerId) {
        jdbcTemplate.update("""
                INSERT INTO directories (project_id, parent_id, name, status, created_by)
                VALUES (?, NULL, ?, 'in_progress', ?)
                """,
                projectId,
                "默认分工目录",
                ownerId
        );
    }

    /**
     * 更新已有项目的可变字段。
     *
     * @param project 已有项目聚合。
     */
    private void update(Project project) {
        jdbcTemplate.update("""
                UPDATE projects
                SET group_id = ?, name = ?, status = ?, ended_at = ?, reopened_at = ?
                WHERE id = ?
                """,
                project.getGroupId(),
                project.getName(),
                project.getStatus().getValue(),
                toTimestamp(project.getEndedAt()),
                toTimestamp(project.getReopenedAt()),
                project.getId()
        );
    }

    /**
     * 填充项目保存语句中的公共字段。
     *
     * @param statement 预编译语句。
     * @param project 待保存项目。
     * @param startIndex 起始参数位置。
     * @throws SQLException 设置参数失败时抛出。
     */
    private void fillSaveStatement(PreparedStatement statement, Project project, int startIndex) throws SQLException {
        statement.setLong(startIndex, project.getGroupId());
        statement.setString(startIndex + 1, project.getName());
        statement.setString(startIndex + 2, project.getStatus().getValue());
        statement.setTimestamp(startIndex + 3, toTimestamp(project.getEndedAt()));
        statement.setTimestamp(startIndex + 4, toTimestamp(project.getReopenedAt()));
    }

    /**
     * 将当前结果集行映射为项目领域对象。
     *
     * @param resultSet 查询结果集。
     * @param rowNumber 当前行号。
     * @return 项目聚合。
     * @throws SQLException 读取字段失败时抛出。
     */
    private Project mapProject(ResultSet resultSet, int rowNumber) throws SQLException {
        return Project.restore(
                resultSet.getLong("id"),
                resultSet.getLong("group_id"),
                resultSet.getLong("owner_id"),
                resultSet.getString("name"),
                toProjectStatus(resultSet.getString("status"))
        );
    }

    /**
     * 将数据库状态值转换为领域枚举。
     *
     * @param value 数据库存储的状态值。
     * @return 项目状态枚举。
     */
    private ProjectStatus toProjectStatus(String value) {
        for (ProjectStatus status : ProjectStatus.values()) {
            if (status.getValue().equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知项目状态：" + value);
    }

    /**
     * 将领域时间转换为 JDBC 时间戳。
     *
     * @param value 领域时间。
     * @return JDBC 时间戳，空值保持为空。
     */
    private Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }
}
