package com.cooperation.domain.file;

import java.util.Objects;

/**
 * 文件展示名值对象，负责阻止路径穿越、路径分隔符和控制字符进入领域模型。
 */
public final class FileName {

    private final String value;

    private FileName(String value) {
        this.value = value;
    }

    /**
     * 创建文件名值对象。
     *
     * @param value 用户上传时提供的原始展示名。
     * @return 校验通过后的文件名值对象。
     * @throws IllegalArgumentException 当文件名为空或包含非法路径字符时抛出。
     */
    public static FileName of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        if (value.contains("..") || value.contains("/") || value.contains("\\")) {
            throw new IllegalArgumentException("文件名不能包含路径信息");
        }
        for (int index = 0; index < value.length(); index++) {
            if (Character.isISOControl(value.charAt(index))) {
                throw new IllegalArgumentException("文件名不能包含控制字符");
            }
        }
        return new FileName(value);
    }

    /**
     * 获取原始展示名。
     *
     * @return 用户上传时的文件展示名。
     */
    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof FileName fileName)) {
            return false;
        }
        return Objects.equals(value, fileName.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
