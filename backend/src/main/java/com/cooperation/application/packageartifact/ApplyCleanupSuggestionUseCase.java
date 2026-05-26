package com.cooperation.application.packageartifact;

import com.cooperation.domain.check.CleanupItem;
import com.cooperation.domain.file.FileAsset;
import com.cooperation.domain.log.OperationAction;
import com.cooperation.domain.log.OperationLog;
import com.cooperation.domain.log.OperationLogRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 应用清理建议的应用用例。
 */
public class ApplyCleanupSuggestionUseCase {

    private final CleanupTargetRepository targets;
    private final CleanupPermissionChecker permissions;
    private final OperationLogRepository logs;

    /**
     * 创建应用清理建议用例。
     *
     * @param targets 清理目标仓储
     * @param permissions 清理权限检查器
     * @param logs 操作记录仓储
     */
    public ApplyCleanupSuggestionUseCase(
            CleanupTargetRepository targets,
            CleanupPermissionChecker permissions,
            OperationLogRepository logs
    ) {
        this.targets = Objects.requireNonNull(targets, "清理目标仓储不能为空");
        this.permissions = Objects.requireNonNull(permissions, "清理权限检查器不能为空");
        this.logs = Objects.requireNonNull(logs, "操作记录仓储不能为空");
    }

    /**
     * 将清理建议中的活动文件移入回收站并记录操作。
     *
     * @param command 清理命令
     * @return 清理结果
     */
    public Result apply(Command command) {
        Objects.requireNonNull(command, "清理命令不能为空");
        permissions.checkCanCleanup(command.projectId(), command.actorId(), command.items());
        List<String> cleanedObjectIds = new ArrayList<>();

        // 逐项按路径定位活动文件；未找到的项目跳过，避免因快照过期阻断其他可清理项。
        for (CleanupItem item : command.items()) {
            targets.findActiveFileByProjectIdAndPath(command.projectId(), item.path()).ifPresent(file -> {
                file.moveToTrash(command.actorId(), LocalDateTime.now());
                targets.save(file);
                cleanedObjectIds.add(file.id());
            });
        }

        logs.save(OperationLog.record(
                command.projectId(),
                command.actorId(),
                OperationAction.CLEANUP_APPLIED,
                "cleanup",
                command.projectId(),
                "应用清理建议",
                Map.of("cleanedCount", String.valueOf(cleanedObjectIds.size())),
                Instant.now()
        ));
        return new Result(cleanedObjectIds);
    }

    /**
     * 预览清理建议命中的活动文件，不修改文件状态或保存对象。
     *
     * @param command 清理预览命令
     * @return 将被清理的对象摘要
     */
    public PreviewResult preview(PreviewCommand command) {
        Objects.requireNonNull(command, "清理预览命令不能为空");
        permissions.checkCanCleanup(command.projectId(), command.actorId(), command.items());
        List<PreviewObject> objects = new ArrayList<>();

        // 逐项按路径定位活动文件；预览只收集摘要，不改变领域对象状态。
        for (CleanupItem item : command.items()) {
            targets.findActiveFileByProjectIdAndPath(command.projectId(), item.path())
                    .map(file -> PreviewObject.from(item.path(), file))
                    .ifPresent(objects::add);
        }
        return new PreviewResult(objects);
    }

    /**
     * 应用清理建议命令。
     *
     * @param projectId 项目标识
     * @param actorId 操作人标识
     * @param items 待清理项目
     */
    public record Command(String projectId, String actorId, List<CleanupItem> items) {

        /**
         * 校验清理命令必填字段。
         */
        public Command {
            projectId = requireText(projectId, "项目标识不能为空");
            actorId = requireText(actorId, "操作人不能为空");
            items = List.copyOf(Objects.requireNonNull(items, "清理项目不能为空"));
        }
    }

    /**
     * 清理建议预览命令。
     *
     * @param projectId 项目标识
     * @param actorId 操作人标识
     * @param items 待预览清理项目
     */
    public record PreviewCommand(String projectId, String actorId, List<CleanupItem> items) {

        /**
         * 校验清理预览命令必填字段。
         */
        public PreviewCommand {
            projectId = requireText(projectId, "项目标识不能为空");
            actorId = requireText(actorId, "操作人不能为空");
            items = List.copyOf(Objects.requireNonNull(items, "清理项目不能为空"));
        }
    }

    /**
     * 应用清理建议结果。
     *
     * @param cleanedObjectIds 已清理对象标识列表
     */
    public record Result(List<String> cleanedObjectIds) {

        /**
         * 防御性复制已清理对象列表。
         */
        public Result {
            cleanedObjectIds = List.copyOf(Objects.requireNonNull(cleanedObjectIds, "已清理对象不能为空"));
        }
    }

    /**
     * 清理建议预览结果。
     *
     * @param objects 将被清理的对象摘要列表
     */
    public record PreviewResult(List<PreviewObject> objects) {

        /**
         * 防御性复制预览对象列表。
         */
        public PreviewResult {
            objects = List.copyOf(Objects.requireNonNull(objects, "预览对象不能为空"));
        }
    }

    /**
     * 清理预览对象摘要。
     *
     * @param path 项目内相对路径
     * @param objectId 文件对象标识
     * @param fileName 文件展示名
     * @param size 文件大小，单位字节
     */
    public record PreviewObject(String path, String objectId, String fileName, long size) {

        /**
         * 从清理路径和活动文件构造预览对象摘要。
         *
         * @param path 项目内相对路径
         * @param file 活动文件
         * @return 清理预览对象摘要
         */
        public static PreviewObject from(String path, FileAsset file) {
            Objects.requireNonNull(file, "预览文件不能为空");
            return new PreviewObject(path, file.id(), file.name().value(), file.size());
        }

        /**
         * 校验预览对象摘要字段。
         */
        public PreviewObject {
            path = requireText(path, "预览路径不能为空");
            objectId = requireText(objectId, "预览对象标识不能为空");
            fileName = requireText(fileName, "预览文件名不能为空");
            if (size < 0) {
                throw new IllegalArgumentException("预览文件大小不能为负数");
            }
        }
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
