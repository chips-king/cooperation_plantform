package com.cooperation.infrastructure.persistence.file;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.cooperation.domain.file.FileAsset;
import com.cooperation.domain.file.FileAssetRepository;
import com.cooperation.domain.file.FileAssetStatus;
import com.cooperation.domain.file.FileName;

/**
 * 基于 {@code file_assets} 表的文件资产仓储实现。
 */
@Repository
public class MyBatisFileAssetRepository implements FileAssetRepository {

    /**
     * 文件资产查询字段，保持字段顺序一致便于复用行映射。
     */
    private static final String SELECT_COLUMNS = """
            SELECT id, project_id, directory_id, name, size, mime_type, storage_key,
                   uploaded_by, version_group_id, version_no, status, deleted_by, deleted_at
            FROM file_assets
            """;

    /**
     * 项目文件查询默认排序，保证同目录同名版本按版本号稳定返回。
     */
    private static final String DEFAULT_ORDER = " ORDER BY directory_id ASC, name ASC, version_no DESC, id ASC";

    /**
     * Spring JDBC 模板，用于执行参数化 SQL。
     */
    private final JdbcTemplate jdbcTemplate;

    /**
     * 文件资产行映射器，集中维护持久化模型到领域模型的转换。
     */
    private final RowMapper<FileAsset> rowMapper = this::mapRow;

    /**
     * 创建文件资产仓储实现。
     *
     * @param jdbcTemplate Spring JDBC 模板。
     */
    public MyBatisFileAssetRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "JDBC 模板不能为空");
    }

    /**
     * 保存文件资产；主键已存在时更新可变状态和元数据。
     *
     * @param fileAsset 待保存的文件资产。
     * @return 保存后的文件资产。
     */
    @Override
    public FileAsset save(FileAsset fileAsset) {
        Objects.requireNonNull(fileAsset, "文件资产不能为空");
        jdbcTemplate.update(
                """
                        INSERT INTO file_assets (
                            id, project_id, directory_id, name, size, mime_type, extension, storage_key,
                            uploaded_by, version_group_id, version_no, status, deleted_at, deleted_by
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE
                            project_id = VALUES(project_id),
                            directory_id = VALUES(directory_id),
                            name = VALUES(name),
                            size = VALUES(size),
                            mime_type = VALUES(mime_type),
                            extension = VALUES(extension),
                            storage_key = VALUES(storage_key),
                            uploaded_by = VALUES(uploaded_by),
                            version_group_id = VALUES(version_group_id),
                            version_no = VALUES(version_no),
                            status = VALUES(status),
                            deleted_at = VALUES(deleted_at),
                            deleted_by = VALUES(deleted_by)
                        """,
                fileAsset.id(),
                parseRequiredLong(fileAsset.projectId(), "项目标识"),
                parseRequiredLong(fileAsset.directoryId(), "目录标识"),
                fileAsset.name().value(),
                fileAsset.size(),
                fileAsset.mimeType(),
                extractExtension(fileAsset.name()),
                fileAsset.storageKey(),
                parseRequiredLong(fileAsset.uploadedBy(), "上传人标识"),
                fileAsset.versionGroupId(),
                fileAsset.versionNo(),
                fileAsset.status().value(),
                fileAsset.deletedAt(),
                parseNullableLong(fileAsset.deletedBy(), "删除人标识")
        );
        return fileAsset;
    }

    /**
     * 按文件标识查询文件资产。
     *
     * @param id 文件唯一标识。
     * @return 匹配文件资产，不存在时返回空。
     */
    @Override
    public Optional<FileAsset> findById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return queryOne(SELECT_COLUMNS + " WHERE id = ?", id);
    }

    /**
     * 查询同目录下处于当前可见状态的同名文件。
     *
     * @param directoryId 目录标识。
     * @param name 文件展示名。
     * @return 匹配文件资产，不存在时返回空。
     */
    @Override
    public Optional<FileAsset> findActiveByDirectoryIdAndName(String directoryId, FileName name) {
        Objects.requireNonNull(name, "文件名不能为空");
        return queryOne(
                SELECT_COLUMNS + " WHERE directory_id = ? AND name = ? AND status = ? ORDER BY version_no DESC, id ASC",
                parseRequiredLong(directoryId, "目录标识"),
                name.value(),
                FileAssetStatus.ACTIVE.value()
        );
    }

    /**
     * 查询项目下当前可见文件。
     *
     * @param projectId 项目标识。
     * @return 当前可见文件集合。
     */
    @Override
    public List<FileAsset> findActiveByProjectId(String projectId) {
        return jdbcTemplate.query(
                SELECT_COLUMNS + " WHERE project_id = ? AND status = ?" + DEFAULT_ORDER,
                rowMapper,
                parseRequiredLong(projectId, "项目标识"),
                FileAssetStatus.ACTIVE.value()
        );
    }

    /**
     * 查询项目回收站文件。
     *
     * @param projectId 项目标识。
     * @return 已移入回收站的文件集合。
     */
    @Override
    public List<FileAsset> findTrashedByProjectId(String projectId) {
        return jdbcTemplate.query(
                SELECT_COLUMNS + " WHERE project_id = ? AND status = ? ORDER BY deleted_at DESC, id ASC",
                rowMapper,
                parseRequiredLong(projectId, "项目标识"),
                FileAssetStatus.TRASHED.value()
        );
    }

    /**
     * 执行单条文件资产查询。
     *
     * @param sql 查询 SQL。
     * @param parameters 查询参数。
     * @return 匹配文件资产，不存在时返回空。
     */
    private Optional<FileAsset> queryOne(String sql, Object... parameters) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, rowMapper, parameters));
        } catch (EmptyResultDataAccessException ignored) {
            return Optional.empty();
        }
    }

    /**
     * 将结果集当前行重建为领域文件资产。
     *
     * @param resultSet 查询结果集。
     * @param rowNumber 当前行号。
     * @return 文件资产领域对象。
     * @throws SQLException 读取结果集字段失败时抛出。
     */
    private FileAsset mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
        return FileAsset.restore(
                resultSet.getString("id"),
                String.valueOf(resultSet.getLong("project_id")),
                String.valueOf(resultSet.getLong("directory_id")),
                FileName.of(resultSet.getString("name")),
                resultSet.getLong("size"),
                resultSet.getString("mime_type"),
                resultSet.getString("storage_key"),
                String.valueOf(resultSet.getLong("uploaded_by")),
                resultSet.getString("version_group_id"),
                resultSet.getInt("version_no"),
                toStatus(resultSet.getString("status")),
                nullableLongAsString(resultSet, "deleted_by"),
                resultSet.getObject("deleted_at", LocalDateTime.class)
        );
    }

    /**
     * 将持久化状态值转换为领域枚举。
     *
     * @param value 数据库中的小写状态值。
     * @return 文件资产状态枚举。
     */
    private FileAssetStatus toStatus(String value) {
        for (FileAssetStatus status : FileAssetStatus.values()) {
            if (status.value().equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知文件状态：" + value);
    }

    /**
     * 将结果集中的可空长整型标识转换为字符串。
     *
     * @param resultSet 查询结果集。
     * @param columnName 字段名。
     * @return 字符串标识，数据库值为空时返回 null。
     * @throws SQLException 读取结果集字段失败时抛出。
     */
    private String nullableLongAsString(ResultSet resultSet, String columnName) throws SQLException {
        long value = resultSet.getLong(columnName);
        if (resultSet.wasNull()) {
            return null;
        }
        return String.valueOf(value);
    }

    /**
     * 将必填字符串标识转换为数据库使用的长整型标识。
     *
     * @param value 字符串标识。
     * @param fieldName 字段中文名。
     * @return 长整型标识。
     */
    private Long parseRequiredLong(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        return parseLong(value, fieldName);
    }

    /**
     * 将可空字符串标识转换为数据库使用的长整型标识。
     *
     * @param value 字符串标识。
     * @param fieldName 字段中文名。
     * @return 长整型标识，输入为空时返回 null。
     */
    private Long parseNullableLong(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return parseLong(value, fieldName);
    }

    /**
     * 将字符串转换为长整型标识，并提供领域可读的错误信息。
     *
     * @param value 字符串标识。
     * @param fieldName 字段中文名。
     * @return 长整型标识。
     */
    private Long parseLong(String value, String fieldName) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(fieldName + "必须是数字", exception);
        }
    }

    /**
     * 从文件名中提取扩展名，供持久化冗余字段使用。
     *
     * @param name 文件展示名。
     * @return 小写扩展名；无扩展名时返回 null。
     */
    private String extractExtension(FileName name) {
        String value = name.value();
        int dotIndex = value.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == value.length() - 1) {
            return null;
        }
        return value.substring(dotIndex + 1).toLowerCase();
    }
}
