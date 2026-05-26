package com.cooperation.domain.packageartifact;

/**
 * 最终压缩包格式。
 */
public enum PackageFormat {

    /** zip 压缩格式，兼容性最好。 */
    ZIP(".zip"),

    /** 7z 压缩格式。 */
    SEVEN_ZIP(".7z"),

    /** tar.gz 压缩格式。 */
    TAR_GZ(".tar.gz");

    private final String extension;

    PackageFormat(String extension) {
        this.extension = extension;
    }

    /**
     * 获取格式对应的文件扩展名。
     *
     * @return 包含点号的扩展名
     */
    public String extension() {
        return extension;
    }
}
