package com.cooperation.domain.file;

import java.util.List;

/**
 * 同目录同名文件处理策略枚举。
 */
public enum DuplicateFilePolicy {

    /**
     * 覆盖旧文件，新文件成为当前文件。
     */
    OVERWRITE {
        @Override
        public DuplicateFileResult apply(FileAsset oldFile, FileAsset newFile) {
            oldFile.markSuperseded();
            newFile.markActive();
            return DuplicateFileResult.of(newFile, List.of(newFile));
        }
    },

    /**
     * 重命名新文件，同时保留旧文件。
     */
    RENAME {
        @Override
        public DuplicateFileResult apply(FileAsset oldFile, FileAsset newFile, FileName renamedName) {
            newFile.renameTo(renamedName);
            newFile.markActive();
            return DuplicateFileResult.of(newFile, List.of(oldFile, newFile));
        }
    },

    /**
     * 将新文件作为同一版本组的新版本。
     */
    NEW_VERSION {
        @Override
        public DuplicateFileResult apply(FileAsset oldFile, FileAsset newFile) {
            oldFile.markSuperseded();
            newFile.keepAsNextVersionOf(oldFile);
            newFile.markActive();
            return DuplicateFileResult.of(newFile, List.of(newFile));
        }
    };

    /**
     * 执行不需要额外文件名的同名处理策略。
     *
     * @param oldFile 已存在的同名文件。
     * @param newFile 本次上传的新文件。
     * @return 同名文件处理结果。
     */
    public DuplicateFileResult apply(FileAsset oldFile, FileAsset newFile) {
        throw new UnsupportedOperationException("该策略需要额外参数");
    }

    /**
     * 执行需要重命名参数的同名处理策略。
     *
     * @param oldFile 已存在的同名文件。
     * @param newFile 本次上传的新文件。
     * @param renamedName 新文件重命名后的展示名。
     * @return 同名文件处理结果。
     */
    public DuplicateFileResult apply(FileAsset oldFile, FileAsset newFile, FileName renamedName) {
        throw new UnsupportedOperationException("该策略不支持重命名参数");
    }
}
