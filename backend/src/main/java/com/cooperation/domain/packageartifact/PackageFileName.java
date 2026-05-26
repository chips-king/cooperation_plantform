package com.cooperation.domain.packageartifact;

import java.util.Objects;

/**
 * 最终压缩包文件名值对象。
 *
 * @param baseName 不含压缩格式后缀的文件名
 * @param format 压缩包格式
 */
public record PackageFileName(String baseName, PackageFormat format) {

    /**
     * 创建压缩包文件名。
     *
     * @param baseName 不含压缩格式后缀的文件名
     * @param format 压缩包格式
     * @return 压缩包文件名值对象
     */
    public static PackageFileName of(String baseName, PackageFormat format) {
        return new PackageFileName(baseName, format);
    }

    /**
     * 校验压缩包文件名和格式。
     */
    public PackageFileName {
        if (baseName == null || baseName.isBlank()) {
            throw new IllegalArgumentException("压缩包文件名不能为空");
        }
        Objects.requireNonNull(format, "压缩包格式不能为空");
        if (containsIllegalCharacter(baseName)) {
            throw new IllegalArgumentException("压缩包文件名包含非法字符");
        }
        baseName = baseName.trim();
    }

    /**
     * 获取带格式后缀的完整压缩包文件名。
     *
     * @return 完整压缩包文件名
     */
    public String fullName() {
        return baseName + format.extension();
    }

    /**
     * 判断文件名是否包含路径或控制字符。
     *
     * @param value 文件名
     * @return 包含非法字符时返回 true
     */
    private static boolean containsIllegalCharacter(String value) {
        if (value.contains("..") || value.contains("/") || value.contains("\\") || value.contains(":")) {
            return true;
        }
        return value.chars().anyMatch(Character::isISOControl);
    }
}
