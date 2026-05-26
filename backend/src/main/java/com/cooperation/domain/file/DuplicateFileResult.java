package com.cooperation.domain.file;

import java.util.List;

/**
 * 同名文件策略执行结果，表达当前文件和列表中应展示的文件集合。
 */
public final class DuplicateFileResult {

    private final FileAsset currentFile;
    private final List<FileAsset> visibleFiles;

    private DuplicateFileResult(FileAsset currentFile, List<FileAsset> visibleFiles) {
        this.currentFile = currentFile;
        this.visibleFiles = List.copyOf(visibleFiles);
    }

    /**
     * 创建同名文件策略结果。
     *
     * @param currentFile 策略执行后成为当前版本的文件。
     * @param visibleFiles 策略执行后文件列表中可见的文件集合。
     * @return 同名文件处理结果。
     */
    public static DuplicateFileResult of(FileAsset currentFile, List<FileAsset> visibleFiles) {
        return new DuplicateFileResult(currentFile, visibleFiles);
    }

    /**
     * 获取当前文件。
     *
     * @return 策略执行后的当前文件。
     */
    public FileAsset currentFile() {
        return currentFile;
    }

    /**
     * 获取可见文件集合。
     *
     * @return 文件列表中应展示的不可变集合。
     */
    public List<FileAsset> visibleFiles() {
        return visibleFiles;
    }
}
