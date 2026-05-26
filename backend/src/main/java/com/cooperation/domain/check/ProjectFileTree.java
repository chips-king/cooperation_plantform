package com.cooperation.domain.check;

import java.util.Arrays;
import java.util.List;

/**
 * 项目文件树快照。
 *
 * @param targets 参与检查的目录和文件目标
 */
public record ProjectFileTree(List<CheckTarget> targets) {

    /**
     * 根据检查目标创建项目文件树。
     *
     * @param targets 检查目标数组
     * @return 项目文件树快照
     */
    public static ProjectFileTree of(CheckTarget... targets) {
        return new ProjectFileTree(Arrays.asList(targets));
    }

    /**
     * 防御性复制文件树目标列表。
     */
    public ProjectFileTree {
        targets = List.copyOf(targets);
    }
}
