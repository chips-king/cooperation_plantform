package com.cooperation.domain.file;

import java.time.LocalDateTime;

/**
 * 文件资产领域实体，维护文件元数据、版本组和回收站状态。
 */
public final class FileAsset {

    private final String id;
    private final String projectId;
    private String directoryId;
    private FileName name;
    private final long size;
    private final String mimeType;
    private final String storageKey;
    private final String uploadedBy;
    private String versionGroupId;
    private int versionNo;
    private FileAssetStatus status;
    private String deletedBy;
    private LocalDateTime deletedAt;

    private FileAsset(
            String id,
            String projectId,
            String directoryId,
            FileName name,
            long size,
            String mimeType,
            String storageKey,
            String uploadedBy,
            String versionGroupId,
            int versionNo
    ) {
        this.id = id;
        this.projectId = projectId;
        this.directoryId = directoryId;
        this.name = name;
        this.size = size;
        this.mimeType = mimeType;
        this.storageKey = storageKey;
        this.uploadedBy = uploadedBy;
        this.versionGroupId = versionGroupId;
        this.versionNo = versionNo;
        this.status = FileAssetStatus.ACTIVE;
    }

    /**
     * 创建已上传文件实体。
     *
     * @param id 文件唯一标识。
     * @param projectId 所属项目标识。
     * @param directoryId 所属目录标识。
     * @param name 文件展示名。
     * @param size 文件大小，单位字节。
     * @param mimeType 文件 MIME 类型。
     * @param storageKey 内部存储键。
     * @param uploadedBy 上传人标识。
     * @param versionGroupId 同名版本组标识。
     * @param versionNo 版本序号。
     * @return 默认处于正常状态的文件实体。
     */
    public static FileAsset uploaded(
            String id,
            String projectId,
            String directoryId,
            FileName name,
            long size,
            String mimeType,
            String storageKey,
            String uploadedBy,
            String versionGroupId,
            int versionNo
    ) {
        return new FileAsset(id, projectId, directoryId, name, size, mimeType, storageKey, uploadedBy, versionGroupId, versionNo);
    }

    /**
     * 从仓储数据重建文件实体，用于基础设施层还原持久化状态。
     *
     * @param id 文件唯一标识。
     * @param projectId 所属项目标识。
     * @param directoryId 所属目录标识。
     * @param name 文件展示名。
     * @param size 文件大小，单位字节。
     * @param mimeType 文件 MIME 类型。
     * @param storageKey 内部存储键。
     * @param uploadedBy 上传人标识。
     * @param versionGroupId 同名版本组标识。
     * @param versionNo 版本序号。
     * @param status 当前文件状态。
     * @param deletedBy 删除人标识，未删除时为空。
     * @param deletedAt 删除时间，未删除时为空。
     * @return 已按持久化状态重建的文件实体。
     */
    public static FileAsset restore(
            String id,
            String projectId,
            String directoryId,
            FileName name,
            long size,
            String mimeType,
            String storageKey,
            String uploadedBy,
            String versionGroupId,
            int versionNo,
            FileAssetStatus status,
            String deletedBy,
            LocalDateTime deletedAt
    ) {
        FileAsset fileAsset = new FileAsset(id, projectId, directoryId, name, size, mimeType, storageKey, uploadedBy, versionGroupId, versionNo);
        fileAsset.status = status;
        fileAsset.deletedBy = deletedBy;
        fileAsset.deletedAt = deletedAt;
        return fileAsset;
    }

    /**
     * 将文件移入回收站。
     *
     * @param deletedBy 删除人标识。
     * @param deletedAt 删除发生时间。
     */
    public void moveToTrash(String deletedBy, LocalDateTime deletedAt) {
        this.status = FileAssetStatus.TRASHED;
        this.deletedBy = deletedBy;
        this.deletedAt = deletedAt;
    }

    /**
     * 从回收站恢复文件。
     */
    public void restore() {
        this.status = FileAssetStatus.ACTIVE;
        this.deletedBy = null;
        this.deletedAt = null;
    }

    /**
     * 从回收站恢复文件并放入指定目录。
     *
     * @param directoryId 恢复目标目录标识。
     */
    public void restoreToDirectory(String directoryId) {
        this.directoryId = directoryId;
        restore();
    }

    /**
     * 判断文件是否处于当前可见状态。
     *
     * @return 当前状态为 active 时返回 true。
     */
    public boolean isActive() {
        return status == FileAssetStatus.ACTIVE;
    }

    /**
     * 获取文件标识。
     *
     * @return 文件唯一标识。
     */
    public String id() {
        return id;
    }

    /**
     * 获取所属项目标识。
     *
     * @return 项目标识。
     */
    public String projectId() {
        return projectId;
    }

    /**
     * 获取所属目录标识。
     *
     * @return 目录标识。
     */
    public String directoryId() {
        return directoryId;
    }

    /**
     * 获取文件展示名。
     *
     * @return 文件名值对象。
     */
    public FileName name() {
        return name;
    }

    /**
     * 获取文件大小。
     *
     * @return 文件大小，单位字节。
     */
    public long size() {
        return size;
    }

    /**
     * 获取 MIME 类型。
     *
     * @return 文件 MIME 类型。
     */
    public String mimeType() {
        return mimeType;
    }

    /**
     * 获取内部存储键。
     *
     * @return 文件存储键。
     */
    public String storageKey() {
        return storageKey;
    }

    /**
     * 获取上传人标识。
     *
     * @return 上传人用户标识。
     */
    public String uploadedBy() {
        return uploadedBy;
    }

    /**
     * 获取版本组标识。
     *
     * @return 同名版本组标识。
     */
    public String versionGroupId() {
        return versionGroupId;
    }

    /**
     * 获取版本序号。
     *
     * @return 文件版本序号。
     */
    public int versionNo() {
        return versionNo;
    }

    /**
     * 获取文件状态。
     *
     * @return 当前文件状态。
     */
    public FileAssetStatus status() {
        return status;
    }

    /**
     * 获取删除人标识。
     *
     * @return 删除人标识，未删除时为 null。
     */
    public String deletedBy() {
        return deletedBy;
    }

    /**
     * 获取删除时间。
     *
     * @return 删除时间，未删除时为 null。
     */
    public LocalDateTime deletedAt() {
        return deletedAt;
    }

    /**
     * 将文件标记为被新文件或新版本替代。
     */
    void markSuperseded() {
        this.status = FileAssetStatus.SUPERSEDED;
    }

    /**
     * 将文件标记为当前可见状态，并清除回收站信息。
     */
    void markActive() {
        this.status = FileAssetStatus.ACTIVE;
        this.deletedBy = null;
        this.deletedAt = null;
    }

    /**
     * 更新文件展示名。
     *
     * @param renamedName 新展示名。
     */
    void renameTo(FileName renamedName) {
        this.name = renamedName;
    }

    /**
     * 继承旧文件版本组并递增版本号。
     *
     * @param oldFile 被新版本替代的旧文件。
     */
    void keepAsNextVersionOf(FileAsset oldFile) {
        this.versionGroupId = oldFile.versionGroupId;
        this.versionNo = oldFile.versionNo + 1;
    }
}
