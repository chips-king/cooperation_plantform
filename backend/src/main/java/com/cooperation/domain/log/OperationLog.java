package com.cooperation.domain.log;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 项目操作记录领域实体。
 */
public class OperationLog {

    /** 项目结束后操作记录默认保留三十天。 */
    public static final Duration DEFAULT_RETENTION_AFTER_PROJECT_ENDED = Duration.ofDays(30);

    private final String projectId;
    private final String actorId;
    private final OperationAction action;
    private final String targetType;
    private final String targetId;
    private final String summary;
    private final Map<String, String> metadata;
    private final Instant createdAt;
    private Instant retainUntil;

    private OperationLog(
            String projectId,
            String actorId,
            OperationAction action,
            String targetType,
            String targetId,
            String summary,
            Map<String, String> metadata,
            Instant createdAt
    ) {
        this.projectId = requireText(projectId, "项目标识不能为空");
        this.actorId = requireText(actorId, "操作人不能为空");
        this.action = Objects.requireNonNull(action, "操作类型不能为空");
        this.targetType = requireText(targetType, "目标类型不能为空");
        this.targetId = requireText(targetId, "目标标识不能为空");
        this.summary = requireText(summary, "操作摘要不能为空");
        this.metadata = Map.copyOf(Objects.requireNonNullElse(metadata, Map.of()));
        this.createdAt = Objects.requireNonNull(createdAt, "操作时间不能为空");
    }

    /**
     * 记录一次项目操作。
     *
     * @param projectId 项目标识
     * @param actorId 操作人标识
     * @param action 操作动作类型
     * @param targetType 目标资源类型
     * @param targetId 目标资源标识
     * @param summary 操作摘要
     * @param metadata 结构化元数据
     * @param createdAt 操作发生时间
     * @return 操作记录实体
     */
    public static OperationLog record(
            String projectId,
            String actorId,
            OperationAction action,
            String targetType,
            String targetId,
            String summary,
            Map<String, String> metadata,
            Instant createdAt
    ) {
        return new OperationLog(projectId, actorId, action, targetType, targetId, summary, metadata, createdAt);
    }

    /**
     * 应用项目结束后的默认保留期。
     *
     * @param endedAt 项目结束时间
     */
    public void applyProjectEndedRetention(Instant endedAt) {
        this.retainUntil = Objects.requireNonNull(endedAt, "项目结束时间不能为空")
                .plus(DEFAULT_RETENTION_AFTER_PROJECT_ENDED);
    }

    /**
     * 获取项目标识。
     *
     * @return 项目标识
     */
    public String getProjectId() {
        return projectId;
    }

    /**
     * 获取操作人标识。
     *
     * @return 操作人标识
     */
    public String getActorId() {
        return actorId;
    }

    /**
     * 获取操作动作类型。
     *
     * @return 操作动作类型
     */
    public OperationAction getAction() {
        return action;
    }

    /**
     * 获取目标资源类型。
     *
     * @return 目标资源类型
     */
    public String getTargetType() {
        return targetType;
    }

    /**
     * 获取目标资源标识。
     *
     * @return 目标资源标识
     */
    public String getTargetId() {
        return targetId;
    }

    /**
     * 获取操作摘要。
     *
     * @return 操作摘要
     */
    public String getSummary() {
        return summary;
    }

    /**
     * 获取结构化元数据。
     *
     * @return 不可变元数据
     */
    public Map<String, String> getMetadata() {
        return metadata;
    }

    /**
     * 获取操作发生时间。
     *
     * @return 操作发生时间
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * 获取保留到期时间。
     *
     * @return 保留到期时间，未设置时为空
     */
    public Optional<Instant> getRetainUntil() {
        return Optional.ofNullable(retainUntil);
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
