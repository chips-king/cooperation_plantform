package com.cooperation.infrastructure.archive;

import com.cooperation.application.packageartifact.PackageArchiveEntry;
import com.cooperation.application.packageartifact.PackageArchivePort;
import com.cooperation.application.packageartifact.PackageArchiveRequest;
import com.cooperation.application.packageartifact.PackageArchiveResult;
import com.cooperation.domain.packageartifact.PackageFormat;
import com.cooperation.infrastructure.storage.StorageProperties;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.springframework.stereotype.Component;

/**
 * 基于 Commons Compress 的 tar.gz 压缩包适配器。
 */
@Component
public class TarGzArchiveAdapter implements PackageArchivePort {

    private static final String PACKAGE_DIRECTORY = "packages";

    private final StorageProperties storageProperties;

    /**
     * 创建 tar.gz 压缩包适配器。
     *
     * @param storageProperties 文件存储配置，用于定位存储根目录
     */
    public TarGzArchiveAdapter(StorageProperties storageProperties) {
        this.storageProperties = Objects.requireNonNull(storageProperties, "文件存储配置不能为空");
    }

    /**
     * 根据打包请求生成 tar.gz 压缩包。
     *
     * @param request 压缩包生成命令，格式必须为 TAR_GZ
     * @return 压缩包生成结果，包含 packages 下的存储键和文件大小
     */
    @Override
    public PackageArchiveResult create(PackageArchiveRequest request) {
        Objects.requireNonNull(request, "压缩包生成请求不能为空");
        if (request.format() != PackageFormat.TAR_GZ) {
            throw new IllegalArgumentException("TarGzArchiveAdapter 仅支持 TAR_GZ 压缩格式");
        }

        Path root = storageProperties.getRoot().toAbsolutePath().normalize();
        Path packageDirectory = root.resolve(PACKAGE_DIRECTORY).resolve(request.packageId()).normalize();
        Path output = resolveOutputPath(root, packageDirectory, request.fileName());

        try {
            Files.createDirectories(packageDirectory);
            writeTarGz(request, root, output);
            return new PackageArchiveResult(toStorageKey(root, output), Files.size(output));
        } catch (IOException ex) {
            throw new IllegalStateException("生成 tar.gz 压缩包失败", ex);
        }
    }

    /**
     * 将请求条目逐个写入 tar.gz 文件。
     *
     * @param request 压缩包生成请求
     * @param root 存储根目录的绝对归一化路径
     * @param output tar.gz 输出文件路径
     * @throws IOException 文件读写失败时抛出
     */
    private void writeTarGz(PackageArchiveRequest request, Path root, Path output) throws IOException {
        OpenOption[] options = {
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        };
        try (OutputStream fileOutput = Files.newOutputStream(output, options);
                BufferedOutputStream bufferedOutput = new BufferedOutputStream(fileOutput);
                GzipCompressorOutputStream gzipOutput = new GzipCompressorOutputStream(bufferedOutput);
                TarArchiveOutputStream tarOutput = new TarArchiveOutputStream(gzipOutput)) {
            tarOutput.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            tarOutput.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);
            for (PackageArchiveEntry entry : request.entries()) {
                // 每个条目独立校验源路径和包内路径，防止读取根目录外文件或写入危险条目名。
                Path source = resolveStoragePath(root, entry.storageKey(), "源文件存储键非法");
                String tarPath = normalizeTarEntryPath(entry.path());
                TarArchiveEntry tarEntry = new TarArchiveEntry(tarPath);
                tarEntry.setSize(Files.size(source));
                tarOutput.putArchiveEntry(tarEntry);
                Files.copy(source, tarOutput);
                tarOutput.closeArchiveEntry();
            }
            tarOutput.finish();
        }
    }

    /**
     * 解析最终压缩包输出路径，并限制在存储根目录的 packages 目录下。
     *
     * @param root 存储根目录的绝对归一化路径
     * @param packageDirectory 压缩包输出目录
     * @param fileName 压缩包文件名
     * @return 最终输出路径
     */
    private Path resolveOutputPath(Path root, Path packageDirectory, String fileName) {
        if (!packageDirectory.toAbsolutePath().normalize().startsWith(root)) {
            throw new IllegalArgumentException("压缩包输出目录必须位于存储根目录内");
        }
        Path output = packageDirectory.resolve(fileName).toAbsolutePath().normalize();
        if (!output.startsWith(packageDirectory) || !output.startsWith(root)) {
            throw new IllegalArgumentException("压缩包输出路径必须位于 packages 目录内");
        }
        return output;
    }

    /**
     * 解析存储键对应的文件路径，并限制在配置根目录内。
     *
     * @param root 存储根目录的绝对归一化路径
     * @param storageKey 内部存储键
     * @param message 路径非法时的错误信息
     * @return 存储键对应的绝对归一化文件路径
     */
    private Path resolveStoragePath(Path root, String storageKey, String message) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        Path path = root.resolve(storageKey.trim()).toAbsolutePath().normalize();
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException(message);
        }
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("源文件不存在或不是普通文件");
        }
        return path;
    }

    /**
     * 归一化 tar 内部条目路径，避免绝对路径、盘符和上级目录进入压缩包。
     *
     * @param path 请求中的 tar 内相对路径
     * @return 使用 {@code /} 分隔的 tar 条目路径
     */
    private String normalizeTarEntryPath(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("压缩包内路径不能为空");
        }
        String normalized = path.trim().replace('\\', '/');
        if (normalized.startsWith("/") || normalized.contains(":")) {
            throw new IllegalArgumentException("压缩包内路径必须是相对路径");
        }
        Path tarPath = Path.of(normalized).normalize();
        if (tarPath.isAbsolute() || tarPath.startsWith("..")) {
            throw new IllegalArgumentException("压缩包内路径不能越过根目录");
        }
        String entryPath = tarPath.toString().replace('\\', '/');
        if (entryPath.isBlank() || ".".equals(entryPath)) {
            throw new IllegalArgumentException("压缩包内路径不能为空");
        }
        return entryPath;
    }

    /**
     * 将输出文件路径转换为相对存储键。
     *
     * @param root 存储根目录的绝对归一化路径
     * @param output 输出文件路径
     * @return 使用 {@code /} 分隔的相对存储键
     */
    private String toStorageKey(Path root, Path output) {
        return root.relativize(output.toAbsolutePath().normalize()).toString().replace('\\', '/');
    }
}
