package com.cooperation.integration.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cooperation.infrastructure.storage.LocalFileStorageAdapter;
import com.cooperation.infrastructure.storage.StorageProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 本地文件存储适配器集成测试，验证真实文件系统上的保存与路径边界行为。
 */
class LocalFileStorageAdapterIT {

    @TempDir
    Path tempDirectory;

    /**
     * 保存文件时，应写入测试根目录下的项目目录与业务目录，并返回可用于定位文件的相对存储键。
     *
     * @throws IOException 读取落盘文件失败时抛出。
     */
    @Test
    void saveShouldWriteContentInsideConfiguredRoot() throws IOException {
        Path root = tempDirectory.resolve("storage-root");
        LocalFileStorageAdapter adapter = newAdapter(root);
        byte[] content = "本地文件内容".getBytes();

        String storageKey = adapter.save("project-1", "directory-1", "report.txt", content);

        Path storedPath = root.resolve(storageKey).normalize();
        assertTrue(storedPath.startsWith(root.toAbsolutePath().normalize()), "文件必须保存到配置根目录内");
        assertTrue(storageKey.startsWith("project-1/directory-1/"), "存储键应包含规范化后的业务路径");
        assertTrue(storageKey.endsWith("-report.txt"), "存储键应保留原始文件名用于追踪");
        assertArrayEquals(content, Files.readAllBytes(storedPath), "落盘内容必须与传入内容一致");
    }

    /**
     * 只有路径穿越标记本身作为业务路径片段时，应拒绝保存，避免向根目录外解析。
     */
    @Test
    void saveShouldRejectTraversalOnlyPathSegments() {
        LocalFileStorageAdapter adapter = newAdapter(tempDirectory.resolve("storage-root"));
        byte[] content = "blocked".getBytes();

        assertThrows(IllegalArgumentException.class, () -> adapter.save("..", "directory-1", "report.txt", content));
        assertThrows(IllegalArgumentException.class, () -> adapter.save("project-1", "..", "report.txt", content));
        assertFalse(Files.exists(tempDirectory.resolve("storage-root")), "拒绝路径穿越时不应创建存储目录");
    }

    /**
     * 带分隔符的危险路径输入应被规范化为普通文件名片段，最终文件仍然限制在测试临时目录内。
     *
     * @throws IOException 读取落盘文件失败时抛出。
     */
    @Test
    void saveShouldNormalizeDangerousPathInputAndStayInsideTempDirectory() throws IOException {
        Path root = tempDirectory.resolve("storage-root");
        LocalFileStorageAdapter adapter = newAdapter(root);
        byte[] content = "normalized".getBytes();

        String storageKey = adapter.save("project/../safe", "dir\\..\\safe", "..\\outside.txt", content);

        Path normalizedTempDirectory = tempDirectory.toAbsolutePath().normalize();
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path storedPath = root.resolve(storageKey).normalize();
        assertTrue(normalizedRoot.startsWith(normalizedTempDirectory), "测试存储根目录必须位于 JUnit 临时目录内");
        assertTrue(storedPath.startsWith(normalizedRoot), "规范化后的文件路径不能逃逸配置根目录");
        assertTrue(storedPath.startsWith(normalizedTempDirectory), "所有文件操作必须限制在测试临时目录内");
        assertFalse(storageKey.contains("\\"), "存储键应统一使用正斜杠，避免暴露平台分隔符");
        assertFalse(Files.exists(tempDirectory.resolve("outside.txt")), "危险文件名不能在测试临时目录根部创建文件");
        assertArrayEquals(content, Files.readAllBytes(storedPath), "规范化后保存的内容必须保持不变");
    }

    /**
     * 按测试指定根目录创建本地文件存储适配器，避免启动 Spring 容器。
     *
     * @param root 测试专用存储根目录，必须位于 {@link TempDir} 临时目录下。
     * @return 已绑定测试根目录的本地文件存储适配器。
     */
    private LocalFileStorageAdapter newAdapter(Path root) {
        StorageProperties properties = new StorageProperties();
        properties.setRoot(root);
        return new LocalFileStorageAdapter(properties);
    }
}
