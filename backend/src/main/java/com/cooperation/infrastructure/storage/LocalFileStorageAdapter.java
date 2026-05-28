package com.cooperation.infrastructure.storage;

import com.cooperation.application.file.FileStoragePort;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Component;

/**
 * 本地文件存储适配器，负责把上传内容保存到配置的本地根目录中。
 */
@Component
public class LocalFileStorageAdapter implements FileStoragePort {

    /**
     * 文件名为空或规范化后不可用时使用的兜底名称。
     */
    private static final String DEFAULT_FILENAME = "file";

    private final StorageProperties properties;

    /**
     * 创建本地文件存储适配器。
     *
     * @param properties 文件存储配置属性。
     */
    public LocalFileStorageAdapter(StorageProperties properties) {
        this.properties = Objects.requireNonNull(properties, "文件存储配置不能为空");
    }

    /**
     * 保存文件内容到 {@code root/projectId/directoryId} 目录，并返回相对存储键。
     *
     * @param projectId 项目标识。
     * @param directoryId 目录标识。
     * @param filename 文件展示名。
     * @param content 文件二进制内容。
     * @return 以 {@code /} 分隔的内部存储键。
     */
    @Override
    public String save(String projectId, String directoryId, String filename, byte[] content) {
        Objects.requireNonNull(content, "文件内容不能为空");

        Path root = properties.getRoot().toAbsolutePath().normalize();
        String projectSegment = normalizeSegment(projectId, "项目标识不能为空");
        String directorySegment = normalizeSegment(directoryId, "目录标识不能为空");
        String storedFilename = UUID.randomUUID() + "-" + normalizeFilename(filename);

        Path directory = root.resolve(projectSegment).resolve(directorySegment).normalize();
        Path target = directory.resolve(storedFilename).normalize();
        ensureInsideRoot(root, directory);
        ensureInsideRoot(root, target);

        try {
            // 先创建受控目录，再一次性写入应用层传入的上传内容。
            Files.createDirectories(directory);
            Files.write(target, content);
        } catch (IOException exception) {
            throw new UncheckedIOException("保存上传文件失败", exception);
        }

        return root.relativize(target).toString().replace('\\', '/');
    }

    private String normalizeSegment(String value, String blankMessage) {
        String normalized = normalizePathPart(value, blankMessage);
        if (normalized.equals(".") || normalized.equals("..")) {
            throw new IllegalArgumentException("路径片段不合法");
        }
        return normalized;
    }

    private String normalizeFilename(String filename) {
        String fallbackName = normalizePathPart(filename, "文件名不能为空");
        try {
            // 只取最后一级名称，避免调用方传入包含路径分隔符的文件名。
            fallbackName = Paths.get(fallbackName).getFileName().toString();
        } catch (InvalidPathException exception) {
            fallbackName = DEFAULT_FILENAME;
        }
        if (fallbackName.equals(".") || fallbackName.equals("..")) {
            return DEFAULT_FILENAME;
        }
        return fallbackName;
    }

    private String normalizePathPart(String value, String blankMessage) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(blankMessage);
        }

        StringBuilder builder = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            // 过滤路径分隔符、控制字符和 Windows 文件名保留字符，降低路径穿越与跨平台非法名称风险。
            if (current == '/' || current == '\\' || current < 32 || ":*?\"<>|".indexOf(current) >= 0) {
                builder.append('_');
            } else {
                builder.append(current);
            }
        }

        String normalized = builder.toString().trim();
        return normalized.isBlank() ? DEFAULT_FILENAME : normalized;
    }

    @Override
    public byte[] load(String storageKey) {
        Objects.requireNonNull(storageKey, "存储键不能为空");
        Path root = properties.getRoot().toAbsolutePath().normalize();
        Path file = root.resolve(storageKey.replace('/', '\\')).normalize();
        ensureInsideRoot(root, file);
        try {
            return Files.readAllBytes(file);
        } catch (IOException exception) {
            throw new UncheckedIOException("读取文件失败", exception);
        }
    }

    private void ensureInsideRoot(Path root, Path target) {
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("文件存储路径必须位于配置根目录内");
        }
    }
}
