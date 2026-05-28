package com.cooperation.application.packageartifact;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * 查询项目所有打包记录应用用例。
 */
public class ListPackagesUseCase {

    private final PackageRepository packageRepository;

    /**
     * 创建查询打包记录用例实例。
     *
     * @param packageRepository 压缩包查询仓储
     */
    public ListPackagesUseCase(PackageRepository packageRepository) {
        this.packageRepository = Objects.requireNonNull(packageRepository, "压缩包仓储不能为空");
    }

    /**
     * 查询项目所有打包记录。
     *
     * @param query 查询参数
     * @return 打包记录列表
     */
    public List<Result> handle(Query query) {
        Objects.requireNonNull(query, "查询参数不能为空");
        return packageRepository.findAllByProjectId(query.projectId());
    }

    /**
     * 查询参数。
     *
     * @param projectId 项目标识
     */
    public record Query(String projectId) {
    }

    /**
     * 打包记录结果。
     *
     * @param packageId 压缩包标识
     * @param filename 文件名
     * @param format 格式
     * @param size 大小（字节）
     * @param createdAt 创建时间
     * @param isLatest 是否为最新包
     */
    public record Result(
            String packageId,
            String filename,
            String format,
            long size,
            Instant createdAt,
            boolean isLatest
    ) {
    }

    /**
     * 压缩包查询仓储。
     */
    public interface PackageRepository {

        /**
         * 查询项目所有打包记录。
         *
         * @param projectId 项目标识
         * @return 打包记录列表
         */
        List<Result> findAllByProjectId(String projectId);
    }
}
