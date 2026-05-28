package com.cooperation.application.packageartifact;

import com.cooperation.domain.log.OperationAction;
import com.cooperation.domain.log.OperationLog;
import com.cooperation.domain.log.OperationLogRepository;
import com.cooperation.domain.packageartifact.PackageFileName;
import com.cooperation.domain.packageartifact.PackageFormat;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 创建最终压缩包的应用用例。
 */
public class CreatePackageUseCase {

    private final PackageSnapshotRepository snapshots;
    private final PackageArchivePort archivePort;
    private final PackageArtifactRepository packages;
    private final OperationLogRepository logs;

    /**
     * 创建最终打包用例。
     *
     * @param snapshots 打包快照仓储
     * @param archivePort 压缩服务端口
     * @param packages 压缩包仓储
     * @param logs 操作记录仓储
     */
    public CreatePackageUseCase(
            PackageSnapshotRepository snapshots,
            PackageArchivePort archivePort,
            PackageArtifactRepository packages,
            OperationLogRepository logs
    ) {
        this.snapshots = Objects.requireNonNull(snapshots, "打包快照仓储不能为空");
        this.archivePort = Objects.requireNonNull(archivePort, "压缩服务端口不能为空");
        this.packages = Objects.requireNonNull(packages, "压缩包仓储不能为空");
        this.logs = Objects.requireNonNull(logs, "操作记录仓储不能为空");
    }

    /**
     * 基于项目当前快照生成最终压缩包。
     *
     * @param command 创建压缩包命令
     * @return 创建压缩包结果
     */
    public Result create(Command command) {
        Objects.requireNonNull(command, "创建压缩包命令不能为空");
        String packageId = UUID.randomUUID().toString();
        String fullName = PackageFileName.of(command.baseName(), command.format()).fullName();
        Instant snapshotCreatedAt = snapshots.snapshotCreatedAt(command.projectId());
        List<PackageArchiveEntry> archiveEntries = snapshots.findSnapshotEntries(command.projectId()).stream()
                .filter(PackageSourceEntry::includedInPackage)
                .map(PackageArchiveEntry::fromSource)
                .toList();
        PackageArchiveResult archiveResult = archivePort.create(
                new PackageArchiveRequest(packageId, fullName, command.format(), snapshotCreatedAt, archiveEntries)
        );
        PackageArtifact artifact = PackageArtifact.create(
                packageId,
                command.projectId(),
                fullName,
                command.format(),
                archiveResult.storageKey(),
                archiveResult.size(),
                command.actorId(),
                Instant.now()
        );
        packages.save(artifact);
        packages.markAsLatest(command.projectId(), packageId);
        logs.save(OperationLog.record(
                command.projectId(),
                command.actorId(),
                OperationAction.PACKAGE_CREATED,
                "package",
                packageId,
                "创建最终压缩包",
                Map.of("fileName", fullName),
                Instant.now()
        ));
        return new Result(
                packageId,
                fullName,
                command.format(),
                snapshotCreatedAt,
                archiveResult.storageKey(),
                archiveResult.size()
        );
    }

    /**
     * 创建压缩包命令。
     *
     * @param projectId 项目标识
     * @param actorId 操作人标识
     * @param baseName 不含扩展名的压缩包文件名
     * @param format 压缩格式
     */
    public record Command(String projectId, String actorId, String baseName, PackageFormat format) {

        /**
         * 校验创建压缩包命令必填字段。
         */
        public Command {
            projectId = requireText(projectId, "项目标识不能为空");
            actorId = requireText(actorId, "操作人不能为空");
            baseName = requireText(baseName, "压缩包基础文件名不能为空");
            Objects.requireNonNull(format, "压缩格式不能为空");
        }
    }

    /**
     * 创建压缩包结果。
     *
     * @param packageId 压缩包标识
     * @param fileName 压缩包文件名
     * @param format 压缩格式
     * @param snapshotCreatedAt 打包快照创建时间
     * @param storageKey 内部存储键
     * @param size 压缩包大小
     */
    public record Result(
            String packageId,
            String fileName,
            PackageFormat format,
            Instant snapshotCreatedAt,
            String storageKey,
            long size
    ) {

        /**
         * 校验创建压缩包结果字段。
         */
        public Result {
            packageId = requireText(packageId, "压缩包标识不能为空");
            fileName = requireText(fileName, "压缩包文件名不能为空");
            Objects.requireNonNull(format, "压缩格式不能为空");
            Objects.requireNonNull(snapshotCreatedAt, "打包快照创建时间不能为空");
            storageKey = requireText(storageKey, "压缩包存储键不能为空");
            if (size < 0) {
                throw new IllegalArgumentException("压缩包大小不能为负数");
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
