package com.cooperation.application.packageartifact;

import java.util.Objects;

/**
 * 删除压缩包应用用例。
 */
public class DeletePackageUseCase {

    private final PackageArtifactRepository packageArtifactRepository;
    private final OperationLogWriter operationLogWriter;

    /**
     * 创建删除压缩包用例实例。
     *
     * @param packageArtifactRepository 压缩包仓储
     * @param operationLogWriter 操作记录写入端口
     */
    public DeletePackageUseCase(
            PackageArtifactRepository packageArtifactRepository,
            OperationLogWriter operationLogWriter
    ) {
        this.packageArtifactRepository = Objects.requireNonNull(packageArtifactRepository, "压缩包仓储不能为空");
        this.operationLogWriter = Objects.requireNonNull(operationLogWriter, "操作记录端口不能为空");
    }

    /**
     * 删除指定压缩包。
     *
     * @param command 删除命令
     */
    public void handle(Command command) {
        Objects.requireNonNull(command, "删除命令不能为空");
        packageArtifactRepository.delete(command.packageId());
        operationLogWriter.record(
                command.projectId(),
                command.actorId(),
                "PACKAGE_DELETED",
                command.packageId()
        );
    }

    /**
     * 删除压缩包命令。
     *
     * @param projectId 项目标识
     * @param packageId 压缩包标识
     * @param actorId 操作人标识
     */
    public record Command(String projectId, String packageId, String actorId) {
    }

    /**
     * 操作记录写入端口。
     */
    public interface OperationLogWriter {

        /**
         * 记录操作日志。
         *
         * @param projectId 项目标识
         * @param actorId 操作人标识
         * @param action 操作动作
         * @param targetId 操作目标标识
         */
        void record(String projectId, String actorId, String action, String targetId);
    }
}
