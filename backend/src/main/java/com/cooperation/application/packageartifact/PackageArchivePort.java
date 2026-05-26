package com.cooperation.application.packageartifact;

/**
 * 最终压缩包生成端口。
 *
 * <p>应用层通过 {@link PackageArchiveRequest#format()} 指定 zip、7z 或 tar.gz，
 * 具体压缩库选择、文件读取和存储写入由基础设施层适配器完成。</p>
 */
public interface PackageArchivePort {

    /**
     * 根据打包请求生成最终压缩包。
     *
     * @param request 压缩包生成命令，包含目标文件名、压缩格式、快照时间和压缩条目
     * @return 压缩包生成结果，包含内部存储键和压缩包大小
     */
    PackageArchiveResult create(PackageArchiveRequest request);
}
