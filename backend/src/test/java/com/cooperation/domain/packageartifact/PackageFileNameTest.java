package com.cooperation.domain.packageartifact;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * 最终压缩包文件名值对象领域测试。
 */
class PackageFileNameTest {

    /**
     * 验证压缩包文件名不能为空。
     */
    @Test
    void shouldRejectBlankPackageFileName() {
        assertThatThrownBy(() -> PackageFileName.of(" ", PackageFormat.ZIP))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("压缩包文件名不能为空");
    }

    /**
     * 验证压缩包文件名不能包含路径分隔符、路径穿越或控制字符。
     */
    @Test
    void shouldRejectIllegalCharactersInPackageFileName() {
        assertThatThrownBy(() -> PackageFileName.of("../final", PackageFormat.ZIP))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("压缩包文件名包含非法字符");

        assertThatThrownBy(() -> PackageFileName.of("team/final", PackageFormat.ZIP))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("压缩包文件名包含非法字符");

        assertThatThrownBy(() -> PackageFileName.of("final\u0000", PackageFormat.ZIP))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("压缩包文件名包含非法字符");
    }

    /**
     * 验证负责人可以指定 zip、7z 和 tar.gz 三种压缩包格式。
     */
    @Test
    void shouldBuildFileNameWithSupportedPackageFormats() {
        assertThat(PackageFileName.of("final-delivery", PackageFormat.ZIP).fullName())
                .isEqualTo("final-delivery.zip");
        assertThat(PackageFileName.of("final-delivery", PackageFormat.SEVEN_ZIP).fullName())
                .isEqualTo("final-delivery.7z");
        assertThat(PackageFileName.of("final-delivery", PackageFormat.TAR_GZ).fullName())
                .isEqualTo("final-delivery.tar.gz");
    }
}
