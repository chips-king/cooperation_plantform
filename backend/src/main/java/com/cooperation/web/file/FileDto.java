package com.cooperation.web.file;

import com.cooperation.domain.file.FileAsset;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 文件管理 Web DTO 集合，集中定义目录树、上传、下载、移动和回收站接口结构。
 */
public final class FileDto {

    private FileDto() {
    }

    /**
     * 目录树响应。
     *
     * @param projectId 项目标识。
     * @param directories 顶层目录节点列表。
     */
    public record DirectoryTreeResponse(String projectId, List<DirectoryNodeResponse> directories) {

        /**
         * 目录节点响应。
         *
         * @param id 目录标识。
         * @param parentId 父目录标识，根目录为空。
         * @param name 目录名称。
         * @param status 目录进度状态。
         * @param files 当前目录下的文件列表。
         * @param children 子目录列表。
         */
        public record DirectoryNodeResponse(
                String id,
                String parentId,
                String name,
                String status,
                List<FileItemResponse> files,
                List<DirectoryNodeResponse> children
        ) {
        }
    }

    /**
     * 文件摘要响应。
     *
     * @param fileId 文件标识。
     * @param name 文件展示名。
     * @param size 文件大小，单位字节。
     * @param mimeType 文件 MIME 类型。
     * @param versionNo 文件版本号。
     * @param status 文件状态。
     * @param uploadedAt 上传时间。
     */
    public record FileItemResponse(
            String fileId,
            String name,
            long size,
            String mimeType,
            int versionNo,
            String status,
            LocalDateTime uploadedAt
    ) {

        /**
         * 从文件领域实体创建文件摘要响应。
         *
         * @param file 文件领域实体。
         * @return 文件摘要响应。
         */
        public static FileItemResponse from(FileAsset file) {
            return new FileItemResponse(
                    file.id(),
                    file.name().value(),
                    file.size(),
                    file.mimeType(),
                    file.versionNo(),
                    file.status().value(),
                    file.uploadedAt()
            );
        }
    }

    /**
     * 上传文件响应。
     *
     * @param fileId 文件标识。
     * @param name 文件展示名。
     * @param size 文件大小，单位字节。
     * @param mimeType 文件 MIME 类型。
     * @param duplicatePolicy 本次使用的同名处理策略。
     * @param versionNo 文件版本号。
     * @param status 文件状态。
     * @param archive 是否为压缩包。
     * @param uploadedAt 上传时间。
     */
    public record UploadFileResponse(
            String fileId,
            String name,
            long size,
            String mimeType,
            String duplicatePolicy,
            int versionNo,
            String status,
            boolean archive,
            LocalDateTime uploadedAt
    ) {
    }

    /**
     * 文件下载响应。
     *
     * @param filename 下载文件名。
     * @param mimeType 文件 MIME 类型。
     * @param content 文件二进制内容。
     */
    public record DownloadResponse(String filename, String mimeType, byte[] content) {
    }

    /**
     * 移动文件请求。
     *
     * @param projectId 项目标识。
     * @param targetDirectoryId 目标目录标识。
     */
    public record MoveFileRequest(String projectId, String targetDirectoryId) {
    }

    /**
     * 创建目录请求。
     *
     * @param projectId 项目标识。
     * @param parentDirectoryId 父目录标识。
     * @param name 目录名称。
     */
    public record CreateDirectoryRequest(String projectId, String parentDirectoryId, String name) {
    }

    /**
     * 删除目录响应。
     *
     * @param parentDirectoryId 被删除目录的父目录标识。
     */
    public record DeleteDirectoryResponse(String parentDirectoryId) {
    }

    /**
     * 回收站文件响应。
     *
     * @param fileId 文件标识。
     * @param name 文件展示名。
     * @param originalDirectoryId 删除前目录标识。
     * @param deletedBy 删除人标识。
     * @param deletedAt 删除时间。
     */
    public record TrashFileResponse(
            String fileId,
            String name,
            String originalDirectoryId,
            String deletedBy,
            Instant deletedAt
    ) {
    }

    /**
     * 恢复文件请求。
     *
     * @param projectId 项目标识。
     * @param restoreDirectoryId 恢复目标目录标识。
     */
    public record RestoreFileRequest(String projectId, String restoreDirectoryId) {
    }
}
