package com.cooperation.infrastructure.persistence.log;

import com.cooperation.domain.log.OperationAction;
import com.cooperation.domain.log.OperationLog;
import com.cooperation.domain.log.OperationLogRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * 基于 operation_logs 表的操作记录仓储实现。
 */
@Repository
public class MyBatisOperationLogRepository implements OperationLogRepository {

    /** 操作记录基础查询语句，统一字段顺序便于复用行映射。 */
    private static final String SELECT_COLUMNS = """
            SELECT project_id, actor_id, action, target_type, target_id, summary, metadata, created_at, retain_until
            FROM operation_logs
            """;

    /** 操作记录查询默认排序，保证同一时间写入时返回顺序稳定。 */
    private static final String DEFAULT_ORDER = " ORDER BY created_at ASC, id ASC";

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<OperationLog> rowMapper = this::mapRow;

    /**
     * 创建操作记录仓储实现。
     *
     * @param jdbcTemplate Spring JDBC 模板
     */
    public MyBatisOperationLogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "JDBC 模板不能为空");
    }

    /**
     * 保存操作记录到 operation_logs 表。
     *
     * @param operationLog 待保存的操作记录
     * @return 保存后的操作记录
     */
    @Override
    public OperationLog save(OperationLog operationLog) {
        Objects.requireNonNull(operationLog, "操作记录不能为空");
        jdbcTemplate.update(
                """
                        INSERT INTO operation_logs (
                            project_id, actor_id, action, target_type, target_id, summary, metadata, created_at, retain_until
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                toDatabaseProjectId(operationLog),
                parseRequiredLong(operationLog.getActorId(), "操作人标识"),
                operationLog.getAction().name(),
                operationLog.getTargetType(),
                operationLog.getTargetId(),
                operationLog.getSummary(),
                toJson(operationLog.getMetadata()),
                Timestamp.from(operationLog.getCreatedAt()),
                operationLog.getRetainUntil().map(Timestamp::from).orElse(null)
        );
        return operationLog;
    }

    /**
     * 按项目标识查询操作记录。
     *
     * @param projectId 项目标识
     * @return 项目下的操作记录列表
     */
    @Override
    public List<OperationLog> findByProjectId(String projectId) {
        return jdbcTemplate.query(
                SELECT_COLUMNS + " WHERE project_id = ?" + DEFAULT_ORDER,
                rowMapper,
                parseRequiredLong(projectId, "项目标识")
        );
    }

    /**
     * 按项目标识和动作类型查询操作记录。
     *
     * @param projectId 项目标识
     * @param action 操作动作类型
     * @return 匹配条件的操作记录列表
     */
    @Override
    public List<OperationLog> findByProjectIdAndAction(String projectId, OperationAction action) {
        Objects.requireNonNull(action, "操作类型不能为空");
        return jdbcTemplate.query(
                SELECT_COLUMNS + " WHERE project_id = ? AND action = ?" + DEFAULT_ORDER,
                rowMapper,
                parseRequiredLong(projectId, "项目标识"),
                action.name()
        );
    }

    /**
     * 按项目标识和操作人标识查询操作记录。
     *
     * @param projectId 项目标识
     * @param actorId 操作人标识
     * @return 匹配条件的操作记录列表
     */
    @Override
    public List<OperationLog> findByProjectIdAndActorId(String projectId, String actorId) {
        return jdbcTemplate.query(
                SELECT_COLUMNS + " WHERE project_id = ? AND actor_id = ?" + DEFAULT_ORDER,
                rowMapper,
                parseRequiredLong(projectId, "项目标识"),
                parseRequiredLong(actorId, "操作人标识")
        );
    }

    /**
     * 按数据库记录标识查询操作记录。
     *
     * @param id 操作记录标识
     * @return 匹配的操作记录
     */
    @Override
    public Optional<OperationLog> findById(String id) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    SELECT_COLUMNS + " WHERE id = ?",
                    rowMapper,
                    parseRequiredLong(id, "操作记录标识")
            ));
        } catch (EmptyResultDataAccessException ignored) {
            return Optional.empty();
        }
    }

    /**
     * 将结果集当前行映射为领域操作记录。
     *
     * @param resultSet 查询结果集
     * @param rowNumber 当前行号
     * @return 领域操作记录
     * @throws SQLException 读取结果集失败时抛出
     */
    private OperationLog mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
        OperationLog operationLog = OperationLog.record(
                String.valueOf(resultSet.getLong("project_id")),
                String.valueOf(resultSet.getLong("actor_id")),
                OperationAction.valueOf(resultSet.getString("action")),
                resultSet.getString("target_type"),
                resultSet.getString("target_id"),
                resultSet.getString("summary"),
                fromJson(resultSet.getString("metadata")),
                toInstant(resultSet.getTimestamp("created_at"))
        );
        Timestamp retainUntil = resultSet.getTimestamp("retain_until");
        if (retainUntil != null) {
            operationLog.applyProjectEndedRetention(retainUntil.toInstant().minus(OperationLog.DEFAULT_RETENTION_AFTER_PROJECT_ENDED));
        }
        return operationLog;
    }

    /**
     * 将必填字符串标识转换为数据库使用的长整型标识。
     *
     * @param value 字符串标识
     * @param fieldName 字段中文名
     * @return 长整型标识
     */
    private Long parseRequiredLong(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(fieldName + "必须是数字", exception);
        }
    }

    private Long toDatabaseProjectId(OperationLog operationLog) {
        if (operationLog.getAction() == OperationAction.GROUP_CREATED) {
            return null;
        }
        return parseRequiredLong(operationLog.getProjectId(), "项目标识");
    }

    /**
     * 将时间戳转换为领域层使用的 Instant。
     *
     * @param timestamp 数据库时间戳
     * @return Instant 时间
     */
    private Instant toInstant(Timestamp timestamp) {
        if (timestamp == null) {
            throw new IllegalArgumentException("操作时间不能为空");
        }
        return timestamp.toInstant();
    }

    /**
     * 将字符串键值元数据转换为 JSON 对象字符串。
     *
     * @param metadata 操作记录元数据
     * @return JSON 对象字符串
     */
    private String toJson(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "{}";
        }
        StringBuilder builder = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            if (!first) {
                builder.append(',');
            }
            first = false;
            builder.append('"')
                    .append(escapeJson(entry.getKey()))
                    .append("\":\"")
                    .append(escapeJson(entry.getValue()))
                    .append('"');
        }
        return builder.append('}').toString();
    }

    /**
     * 将 JSON 对象字符串转换为字符串键值元数据。
     *
     * @param json JSON 对象字符串
     * @return 元数据键值对
     */
    private Map<String, String> fromJson(String json) {
        Map<String, String> metadata = new LinkedHashMap<>();
        if (json == null || json.isBlank() || "{}".equals(json.trim())) {
            return metadata;
        }
        String content = json.trim();
        if (!content.startsWith("{") || !content.endsWith("}")) {
            return Map.of("raw", content);
        }
        int index = 1;
        while (index < content.length() - 1) {
            index = skipWhitespaceAndComma(content, index);
            if (index >= content.length() - 1) {
                break;
            }
            ParsedJsonString key = readJsonString(content, index);
            index = skipWhitespace(content, key.nextIndex());
            if (index >= content.length() || content.charAt(index) != ':') {
                return Map.of("raw", content);
            }
            ParsedJsonString value = readJsonString(content, skipWhitespace(content, index + 1));
            metadata.put(key.value(), value.value());
            index = value.nextIndex();
        }
        return metadata;
    }

    /**
     * 跳过 JSON 成员之间的空白和逗号。
     *
     * @param content JSON 内容
     * @param index 起始下标
     * @return 下一个有效字符下标
     */
    private int skipWhitespaceAndComma(String content, int index) {
        int current = index;
        while (current < content.length() && (Character.isWhitespace(content.charAt(current)) || content.charAt(current) == ',')) {
            current++;
        }
        return current;
    }

    /**
     * 跳过 JSON 分隔符附近的空白字符。
     *
     * @param content JSON 内容
     * @param index 起始下标
     * @return 下一个非空白字符下标
     */
    private int skipWhitespace(String content, int index) {
        int current = index;
        while (current < content.length() && Character.isWhitespace(content.charAt(current))) {
            current++;
        }
        return current;
    }

    /**
     * 读取一个 JSON 字符串字面量。
     *
     * @param content JSON 内容
     * @param index 起始下标
     * @return 解析出的字符串和下一个读取下标
     */
    private ParsedJsonString readJsonString(String content, int index) {
        if (index >= content.length() || content.charAt(index) != '"') {
            return new ParsedJsonString(content.substring(Math.min(index, content.length())), content.length());
        }
        StringBuilder builder = new StringBuilder();
        int current = index + 1;
        boolean escaped = false;
        while (current < content.length()) {
            char value = content.charAt(current);
            if (escaped) {
                builder.append(unescapeJson(value));
                escaped = false;
            } else if (value == '\\') {
                escaped = true;
            } else if (value == '"') {
                return new ParsedJsonString(builder.toString(), current + 1);
            } else {
                builder.append(value);
            }
            current++;
        }
        return new ParsedJsonString(builder.toString(), current);
    }

    /**
     * 转义 JSON 字符串中的特殊字符。
     *
     * @param value 原始字符串
     * @return 转义后的字符串
     */
    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * 还原 JSON 字符串中的简单转义字符。
     *
     * @param value 转义标记字符
     * @return 还原后的字符
     */
    private char unescapeJson(char value) {
        return switch (value) {
            case 'n' -> '\n';
            case 'r' -> '\r';
            case 't' -> '\t';
            default -> value;
        };
    }

    /**
     * JSON 字符串解析结果。
     *
     * @param value 字符串值
     * @param nextIndex 下一个读取下标
     */
    private record ParsedJsonString(String value, int nextIndex) {
    }
}
