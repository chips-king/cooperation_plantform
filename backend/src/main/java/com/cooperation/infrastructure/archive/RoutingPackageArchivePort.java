package com.cooperation.infrastructure.archive;

import com.cooperation.application.packageartifact.PackageArchivePort;
import com.cooperation.application.packageartifact.PackageArchiveRequest;
import com.cooperation.application.packageartifact.PackageArchiveResult;
import com.cooperation.domain.packageartifact.PackageFormat;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * 按压缩格式路由到对应压缩适配器，避免所有格式都走 zip 实现。
 */
@Component
@Primary
public class RoutingPackageArchivePort implements PackageArchivePort {

    private final Map<PackageFormat, PackageArchivePort> adapters;

    /**
     * 创建格式路由压缩端口。
     *
     * @param zipAdapter zip 压缩适配器
     * @param sevenZipAdapter 7z 压缩适配器
     * @param tarGzAdapter tar.gz 压缩适配器
     */
    public RoutingPackageArchivePort(
            ZipArchiveAdapter zipAdapter,
            SevenZipArchiveAdapter sevenZipAdapter,
            TarGzArchiveAdapter tarGzAdapter
    ) {
        Map<PackageFormat, PackageArchivePort> mapping = new EnumMap<>(PackageFormat.class);
        mapping.put(PackageFormat.ZIP, Objects.requireNonNull(zipAdapter, "zip 压缩适配器不能为空"));
        mapping.put(PackageFormat.SEVEN_ZIP, Objects.requireNonNull(sevenZipAdapter, "7z 压缩适配器不能为空"));
        mapping.put(PackageFormat.TAR_GZ, Objects.requireNonNull(tarGzAdapter, "tar.gz 压缩适配器不能为空"));
        this.adapters = Map.copyOf(mapping);
    }

    /**
     * 根据请求格式选择对应压缩适配器并生成压缩包。
     *
     * @param request 压缩包生成请求
     * @return 压缩包生成结果
     */
    @Override
    public PackageArchiveResult create(PackageArchiveRequest request) {
        Objects.requireNonNull(request, "压缩包生成请求不能为空");
        PackageArchivePort adapter = adapters.get(request.format());
        if (adapter == null) {
            throw new IllegalArgumentException("不支持的压缩格式: " + request.format());
        }
        return adapter.create(request);
    }
}
