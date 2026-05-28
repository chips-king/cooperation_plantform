package com.cooperation.application.packageartifact;

/**
 * 最终压缩包仓储抽象。
 */
public interface PackageArtifactRepository {

    /**
     * 保存最终压缩包记录。
     *
     * @param artifact 最终压缩包记录
     * @return 保存后的最终压缩包记录
     */
    PackageArtifact save(PackageArtifact artifact);

    /**
     * 将指定压缩包标记为项目最近一次压缩包。
     *
     * @param projectId 项目标识
     * @param packageId 压缩包标识
     */
    void markAsLatest(String projectId, String packageId);

    /**
     * 删除指定压缩包。
     *
     * @param packageId 压缩包标识
     */
    void delete(String packageId);
}
