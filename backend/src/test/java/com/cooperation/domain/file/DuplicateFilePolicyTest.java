package com.cooperation.domain.file;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 同名文件处理策略领域规则测试。
 */
class DuplicateFilePolicyTest {

    /**
     * 验证覆盖策略会让新文件成为当前文件，并将旧文件标记为被覆盖。
     */
    @Test
    void shouldSupersedeOldFileWhenOverwriteDuplicateFile() {
        FileAsset oldFile = uploadedFile("file-1", "report.docx", "group-1", 1);
        FileAsset newFile = uploadedFile("file-2", "report.docx", "group-2", 1);

        DuplicateFileResult result = DuplicateFilePolicy.OVERWRITE.apply(oldFile, newFile);

        assertThat(result.currentFile()).isSameAs(newFile);
        assertThat(result.visibleFiles()).containsExactly(newFile);
        assertThat(oldFile.status()).isEqualTo(FileAssetStatus.SUPERSEDED);
        assertThat(newFile.status()).isEqualTo(FileAssetStatus.ACTIVE);
    }

    /**
     * 验证重命名策略会保留原文件，并让新文件使用指定的新展示名。
     */
    @Test
    void shouldKeepBothFilesWhenRenameDuplicateFile() {
        FileAsset oldFile = uploadedFile("file-1", "report.docx", "group-1", 1);
        FileAsset newFile = uploadedFile("file-2", "report.docx", "group-2", 1);

        DuplicateFileResult result = DuplicateFilePolicy.RENAME.apply(
                oldFile,
                newFile,
                FileName.of("report (1).docx")
        );

        assertThat(result.currentFile()).isSameAs(newFile);
        assertThat(result.visibleFiles()).containsExactly(oldFile, newFile);
        assertThat(oldFile.status()).isEqualTo(FileAssetStatus.ACTIVE);
        assertThat(newFile.name().value()).isEqualTo("report (1).docx");
        assertThat(newFile.status()).isEqualTo(FileAssetStatus.ACTIVE);
    }

    /**
     * 验证保留新版本策略会共用版本组，并让新文件成为该组最新版本。
     */
    @Test
    void shouldCreateNewVersionWhenKeepDuplicateFileAsNewVersion() {
        FileAsset oldFile = uploadedFile("file-1", "report.docx", "version-group-1", 1);
        FileAsset newFile = uploadedFile("file-2", "report.docx", "version-group-2", 1);

        DuplicateFileResult result = DuplicateFilePolicy.NEW_VERSION.apply(oldFile, newFile);

        assertThat(result.currentFile()).isSameAs(newFile);
        assertThat(result.visibleFiles()).containsExactly(newFile);
        assertThat(oldFile.status()).isEqualTo(FileAssetStatus.SUPERSEDED);
        assertThat(newFile.status()).isEqualTo(FileAssetStatus.ACTIVE);
        assertThat(newFile.versionGroupId()).isEqualTo(oldFile.versionGroupId());
        assertThat(newFile.versionNo()).isEqualTo(2);
    }

    /**
     * 创建文件实体测试样本，保持同名策略用例只关注领域状态变化。
     */
    private FileAsset uploadedFile(String id, String name, String versionGroupId, int versionNo) {
        return FileAsset.uploaded(
                id,
                "project-1",
                "directory-1",
                FileName.of(name),
                1024L,
                "application/octet-stream",
                "project-files/" + id,
                "user-1",
                versionGroupId,
                versionNo
        );
    }
}
