package com.cooperation.integration.archive;

import static org.assertj.core.api.Assertions.assertThat;

import com.cooperation.application.packageartifact.PackageArchiveEntry;
import com.cooperation.application.packageartifact.PackageArchiveRequest;
import com.cooperation.application.packageartifact.PackageArchiveResult;
import com.cooperation.domain.packageartifact.PackageFormat;
import com.cooperation.infrastructure.archive.SevenZipArchiveAdapter;
import com.cooperation.infrastructure.archive.TarGzArchiveAdapter;
import com.cooperation.infrastructure.archive.ZipArchiveAdapter;
import com.cooperation.infrastructure.storage.StorageProperties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 打包归档适配器集成测试，使用临时目录直接验证真实文件归档能力。
 */
class PackageArchiveIT {

    private static final Instant SNAPSHOT_CREATED_AT = Instant.parse("2026-05-25T00:00:00Z");

    @TempDir
    private Path tempDir;

    /**
     * 验证 zip 归档能够生成文件，并且包内条目不额外包裹项目根目录。
     *
     * @throws IOException 临时文件准备或 zip 读取失败时抛出
     */
    @Test
    @DisplayName("zip 打包应生成文件且不额外套项目根目录")
    void shouldCreateZipWithoutProjectRootDirectory() throws IOException {
        PackageArchiveRequest request = createRequest(PackageFormat.ZIP, "archive.zip");
        PackageArchiveResult result = new ZipArchiveAdapter(createStorageProperties()).create(request);

        Path archive = resolveArchive(result);

        assertThat(Files.isRegularFile(archive)).isTrue();
        assertThat(Files.size(archive)).isGreaterThan(0L);
        assertThat(readZipEntryNames(archive))
                .containsExactlyInAnyOrder("src/Main.java", "docs/readme.md")
                .noneMatch(name -> name.startsWith("demo-project/"));
    }

    /**
     * 验证 tar.gz 归档能够生成文件，并且包内条目不额外包裹项目根目录。
     *
     * @throws IOException 临时文件准备或 tar.gz 读取失败时抛出
     */
    @Test
    @DisplayName("tar.gz 打包应生成文件且不额外套项目根目录")
    void shouldCreateTarGzWithoutProjectRootDirectory() throws IOException {
        PackageArchiveRequest request = createRequest(PackageFormat.TAR_GZ, "archive.tar.gz");
        PackageArchiveResult result = new TarGzArchiveAdapter(createStorageProperties()).create(request);

        Path archive = resolveArchive(result);

        assertThat(Files.isRegularFile(archive)).isTrue();
        assertThat(Files.size(archive)).isGreaterThan(0L);
        assertThat(readTarGzEntryNames(archive))
                .containsExactlyInAnyOrder("src/Main.java", "docs/readme.md")
                .noneMatch(name -> name.startsWith("demo-project/"));
    }

    /**
     * 验证 7z 归档能够生成非空文件，避免依赖外部解压工具或 Docker。
     *
     * @throws IOException 临时文件准备或文件大小读取失败时抛出
     */
    @Test
    @DisplayName("7z 打包应生成非空文件")
    void shouldCreateSevenZipArchiveFile() throws IOException {
        PackageArchiveRequest request = createRequest(PackageFormat.SEVEN_ZIP, "archive.7z");
        PackageArchiveResult result = new SevenZipArchiveAdapter(createStorageProperties()).create(request);

        Path archive = resolveArchive(result);

        assertThat(Files.isRegularFile(archive)).isTrue();
        assertThat(Files.size(archive)).isGreaterThan(0L);
        assertThat(result.size()).isGreaterThan(0L);
    }

    /**
     * 创建指向 JUnit 临时目录的存储配置。
     *
     * @return 归档适配器可直接使用的存储配置
     */
    private StorageProperties createStorageProperties() {
        StorageProperties storageProperties = new StorageProperties();
        storageProperties.setRoot(tempDir);
        return storageProperties;
    }

    /**
     * 准备带项目根目录的源文件，并创建不包含项目根目录的归档请求。
     *
     * @param format 归档格式
     * @param fileName 输出归档文件名
     * @return 可提交给归档适配器的请求
     * @throws IOException 源文件创建失败时抛出
     */
    private PackageArchiveRequest createRequest(PackageFormat format, String fileName) throws IOException {
        PackageArchiveEntry mainFile = createEntry(
                "demo-project/src/Main.java",
                "src/Main.java",
                "class Main {}\n");
        PackageArchiveEntry readmeFile = createEntry(
                "demo-project/docs/readme.md",
                "docs/readme.md",
                "# Demo\n");
        return new PackageArchiveRequest(fileName, format, SNAPSHOT_CREATED_AT, List.of(mainFile, readmeFile));
    }

    /**
     * 创建单个源文件并转换为归档条目。
     *
     * @param storageKey 源文件在存储根目录下的键
     * @param archivePath 文件进入压缩包后的相对路径
     * @param content 源文件内容
     * @return 带有实际文件大小的归档条目
     * @throws IOException 文件写入失败时抛出
     */
    private PackageArchiveEntry createEntry(String storageKey, String archivePath, String content) throws IOException {
        Path source = tempDir.resolve(storageKey).normalize();
        Files.createDirectories(source.getParent());
        Files.writeString(source, content, StandardCharsets.UTF_8);
        return new PackageArchiveEntry(archivePath, storageKey, Files.size(source));
    }

    /**
     * 根据归档结果解析实际输出文件路径。
     *
     * @param result 归档适配器返回结果
     * @return 临时存储根目录下的归档文件路径
     */
    private Path resolveArchive(PackageArchiveResult result) {
        return tempDir.resolve(result.storageKey()).normalize();
    }

    /**
     * 读取 zip 文件内所有条目名称。
     *
     * @param archive zip 归档文件路径
     * @return zip 内条目名称列表
     * @throws IOException zip 文件读取失败时抛出
     */
    private List<String> readZipEntryNames(Path archive) throws IOException {
        List<String> names = new ArrayList<>();
        try (InputStream input = Files.newInputStream(archive);
                ZipInputStream zipInput = new ZipInputStream(input)) {
            ZipEntry entry;
            while ((entry = zipInput.getNextEntry()) != null) {
                // 只检查条目路径结构，内容校验由适配器单元层和文件大小断言覆盖。
                names.add(entry.getName());
            }
        }
        return names;
    }

    /**
     * 读取 tar.gz 文件内所有条目名称。
     *
     * @param archive tar.gz 归档文件路径
     * @return tar.gz 内条目名称列表
     * @throws IOException tar.gz 文件读取失败时抛出
     */
    private List<String> readTarGzEntryNames(Path archive) throws IOException {
        List<String> names = new ArrayList<>();
        try (InputStream input = Files.newInputStream(archive);
                GzipCompressorInputStream gzipInput = new GzipCompressorInputStream(input);
                TarArchiveInputStream tarInput = new TarArchiveInputStream(gzipInput)) {
            TarArchiveEntry entry;
            while ((entry = tarInput.getNextEntry()) != null) {
                // 只检查条目路径结构，确保没有被额外包进源项目目录。
                names.add(entry.getName());
            }
        }
        return names;
    }
}
