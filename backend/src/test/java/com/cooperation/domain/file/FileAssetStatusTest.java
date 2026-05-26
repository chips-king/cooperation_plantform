package com.cooperation.domain.file;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 文件状态转换领域规则测试。
 */
class FileAssetStatusTest {

    /**
     * 验证新上传文件默认处于 active 状态。
     */
    @Test
    void shouldBeActiveWhenFileUploaded() {
        FileAsset file = uploadedFile("file-1", "report.docx", "version-group-1", 1);

        assertThat(file.status()).isEqualTo(FileAssetStatus.ACTIVE);
    }

    /**
     * 验证删除文件会进入回收站，并记录删除人和删除时间。
     */
    @Test
    void shouldMoveActiveFileToTrashWhenDeleted() {
        FileAsset file = uploadedFile("file-1", "report.docx", "version-group-1", 1);
        LocalDateTime deletedAt = LocalDateTime.of(2026, 5, 24, 16, 30);

        file.moveToTrash("user-2", deletedAt);

        assertThat(file.status()).isEqualTo(FileAssetStatus.TRASHED);
        assertThat(file.deletedBy()).isEqualTo("user-2");
        assertThat(file.deletedAt()).isEqualTo(deletedAt);
    }

    /**
     * 验证回收站文件可以恢复为 active，并清空删除信息。
     */
    @Test
    void shouldRestoreTrashedFileToActive() {
        FileAsset file = uploadedFile("file-1", "report.docx", "version-group-1", 1);
        file.moveToTrash("user-2", LocalDateTime.of(2026, 5, 24, 16, 30));

        file.restore();

        assertThat(file.status()).isEqualTo(FileAssetStatus.ACTIVE);
        assertThat(file.deletedBy()).isNull();
        assertThat(file.deletedAt()).isNull();
    }

    /**
     * 验证覆盖同名文件后旧文件不再处于 active 状态。
     */
    @Test
    void shouldMarkOldFileNotActiveAfterOverwrite() {
        FileAsset oldFile = uploadedFile("file-1", "report.docx", "version-group-1", 1);
        FileAsset newFile = uploadedFile("file-2", "report.docx", "version-group-2", 1);

        DuplicateFilePolicy.OVERWRITE.apply(oldFile, newFile);

        assertThat(oldFile.status()).isEqualTo(FileAssetStatus.SUPERSEDED);
        assertThat(oldFile.isActive()).isFalse();
        assertThat(newFile.status()).isEqualTo(FileAssetStatus.ACTIVE);
        assertThat(newFile.isActive()).isTrue();
    }

    /**
     * 创建文件实体测试样本，便于状态转换用例复用一致的初始状态。
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
