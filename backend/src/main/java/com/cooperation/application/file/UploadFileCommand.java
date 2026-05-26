package com.cooperation.application.file;

import com.cooperation.domain.file.DuplicateFilePolicy;

/**
 * 上传文件命令，承载应用层完成文件保存所需的全部输入。
 *
 * @param projectId 项目标识。
 * @param directoryId 目标目录标识。
 * @param uploadedBy 上传人用户标识。
 * @param originalFilename 用户选择的原始文件名。
 * @param size 文件大小，单位字节。
 * @param mimeType 文件 MIME 类型。
 * @param archive 是否为压缩包文件。
 * @param duplicatePolicy 同名文件处理策略。
 * @param renamedFilename 重命名策略下的新文件名。
 * @param content 文件二进制内容。
 */
public record UploadFileCommand(
        String projectId,
        String directoryId,
        String uploadedBy,
        String originalFilename,
        long size,
        String mimeType,
        boolean archive,
        DuplicateFilePolicy duplicatePolicy,
        String renamedFilename,
        byte[] content
) {
}
