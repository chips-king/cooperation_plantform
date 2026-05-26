package com.cooperation.infrastructure.storage;

import java.nio.file.Path;
import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DataSizeUnit;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;
import org.springframework.util.unit.DataUnit;

/**
 * 文件存储配置属性，绑定 {@code app.file-storage} 下的存储根路径、单文件大小限制和临时目录。
 */
@Component
@ConfigurationProperties(prefix = "app.file-storage")
public class StorageProperties {

    /**
     * 文件存储根路径，不能为空；建议取值为应用可控的本地持久化目录，默认 {@code ./data/storage}。
     */
    private Path root = Path.of("./data/storage");

    /**
     * 单文件大小限制，必须大于 0；未写单位时按 MB 解析，默认 100MB。
     */
    @DataSizeUnit(DataUnit.MEGABYTES)
    private DataSize singleFileSizeLimit = DataSize.ofMegabytes(100);

    /**
     * 临时文件目录，可为空；为空时使用 {@code root/tmp}，配置时建议位于存储根路径或应用可控目录内。
     */
    private Path tempDirectory;

    /**
     * 获取文件存储根路径。
     *
     * @return 文件存储根路径，永远不返回 {@code null}
     */
    public Path getRoot() {
        return root;
    }

    /**
     * 设置文件存储根路径。
     *
     * @param root 文件存储根路径，不能为空
     */
    public void setRoot(Path root) {
        this.root = Objects.requireNonNull(root, "文件存储根路径不能为空");
    }

    /**
     * 获取单文件大小限制。
     *
     * @return 单文件大小限制，必须大于 0
     */
    public DataSize getSingleFileSizeLimit() {
        return singleFileSizeLimit;
    }

    /**
     * 设置单文件大小限制。
     *
     * @param singleFileSizeLimit 单文件大小限制，必须大于 0
     */
    public void setSingleFileSizeLimit(DataSize singleFileSizeLimit) {
        DataSize actualLimit = Objects.requireNonNull(singleFileSizeLimit, "单文件大小限制不能为空");
        if (actualLimit.toBytes() <= 0) {
            throw new IllegalArgumentException("单文件大小限制必须大于 0");
        }
        this.singleFileSizeLimit = actualLimit;
    }

    /**
     * 获取临时文件目录。
     *
     * @return 已配置的临时目录；未配置时返回 {@code root/tmp}
     */
    public Path getTempDirectory() {
        // 未单独配置临时目录时，跟随根路径变化，避免默认临时文件落到不可控位置。
        return tempDirectory == null ? root.resolve("tmp") : tempDirectory;
    }

    /**
     * 设置临时文件目录。
     *
     * @param tempDirectory 临时文件目录，可为空；为空时使用 {@code root/tmp}
     */
    public void setTempDirectory(Path tempDirectory) {
        this.tempDirectory = tempDirectory;
    }
}
