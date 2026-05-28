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
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.stereotype.Component;

/**
 * 基于 JDK {@link ZipOutputStream} 的 zip 压缩包适配器。
 */
@Component
public class ZipArchiveAdapter implements PackageArchivePort {

    private static final String PACKAGE_DIRECTORY = "packages";

    private final StorageProperties storageProperties;

    /**
     * 创建 zip 压缩包适配器。
     *
     * @param storageProperties 文件存储配置，用于定位存储根目录
     */
    public ZipArchiveAdapter(StorageProperties storageProperties) {
        this.storageProperties = Objects.requireNonNull(storageProperties, "文件存储配置不能为空");
    }

    /**
     * 根据打包请求生成 zip 压缩包。
     *
     * @param request 压缩包生成命令，格式必须为 ZIP
     * @return 压缩包生成结果，包含 packages 下的存储键和文件大小
     */
    @Override
    public PackageArchiveResult create(PackageArchiveRequest request) {
        Objects.requireNonNull(request, "压缩包生成请求不能为空");
        if (request.format() != PackageFormat.ZIP) {
            throw new IllegalArgumentException("ZipArchiveAdapter 仅支持 ZIP 压缩格式");
        }

        Path root = storageProperties.getRoot().toAbsolutePath().normalize();
        Path packageDirectory = root.resolve(PACKAGE_DIRECTORY).resolve(request.packageId()).normalize();
        Path output = resolveOutputPath(packageDirectory, request.fileName());

        try {
            Files.createDirectories(packageDirectory);
            writeZip(request, root, output);
            return new PackageArchiveResult(toStorageKey(root, output), Files.size(output));
        } catch (IOException ex) {
            throw new IllegalStateException(
                    "生成 zip 压缩包失败: " + ex.getMessage() + " (输出路径=" + output + ")", ex);
        }
    }

    /**
     * 将请求条目逐个写入 zip 文件。
     *
     * @param request 压缩包生成请求
     * @param root 存储根目录的绝对归一化路径
     * @param output zip 输出文件路径
     * @throws IOException 文件读写失败时抛出
     */
    private void writeZip(PackageArchiveRequest request, Path root, Path output) throws IOException {
        OpenOption[] options = {
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        };
        try (OutputStream fileOutput = Files.newOutputStream(output, options);
                ZipOutputStream zipOutput = new ZipOutputStream(new BufferedOutputStream(fileOutput))) {
            for (PackageArchiveEntry entry : request.entries()) {
                // 每个条目独立校验源路径和压缩包内路径，防止路径穿越和绝对路径写入。
                Path source = resolveStoragePath(root, entry.storageKey(), "源文件存储键非法");
                String zipPath = normalizeZipEntryPath(entry.path());
                try {
                    zipOutput.putNextEntry(new ZipEntry(zipPath));
                    Files.copy(source, zipOutput);
                    zipOutput.closeEntry();
                } catch (IOException ex) {
                    throw new IOException(
                            "写入 zip 条目失败: zipPath=" + zipPath + ", source=" + source + ", cause=" + ex.getMessage(), ex);
                }
            }
        }
    }

    /**
     * 解析最终压缩包输出路径，并限制在 packages 目录下。
     *
     * @param packageDirectory 压缩包输出目录
     * @param fileName 压缩包文件名
     * @return 最终输出路径
     */
    private Path resolveOutputPath(Path packageDirectory, String fileName) {
        Path output = packageDirectory.resolve(fileName).normalize();
        if (!output.startsWith(packageDirectory)) {
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
            throw new IllegalArgumentException(message + " (storageKey=" + storageKey + ")");
        }
        Path path = root.resolve(storageKey.trim()).toAbsolutePath().normalize();
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException(message + " (路径越界: " + path + ")");
        }
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("源文件不存在或不是普通文件 (path=" + path + ")");
        }
        return path;
    }

    /**
     * 归一化 zip 内部条目路径，避免绝对路径、盘符和上级目录进入压缩包。
     *
     * @param path 请求中的 zip 内相对路径
     * @return 使用 {@code /} 分隔的 zip 条目路径
     */
    private String normalizeZipEntryPath(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("压缩包内路径不能为空");
        }
        String normalized = path.trim().replace('\\', '/');
        if (normalized.startsWith("/") || normalized.contains(":")) {
            throw new IllegalArgumentException("压缩包内路径必须是相对路径");
        }
        Path zipPath = Path.of(normalized).normalize();
        if (zipPath.isAbsolute() || zipPath.startsWith("..")) {
            throw new IllegalArgumentException("压缩包内路径不能越过根目录");
        }
        String entryPath = zipPath.toString().replace('\\', '/');
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
