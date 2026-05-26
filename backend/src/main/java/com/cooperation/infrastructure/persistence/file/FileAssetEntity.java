package com.cooperation.infrastructure.persistence.file;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 文件资产持久化实体，对应数据库 {@code file_assets} 表。
 */
@TableName("file_assets")
public class FileAssetEntity {

    /**
     * 文件标识，对应自增主键 {@code id}。
     */
    @TableId("id")
    private String id;

    /**
     * 所属项目标识，对应 {@code project_id}。
     */
    @TableField("project_id")
    private Long projectId;

    /**
     * 所属目录标识，对应 {@code directory_id}。
     */
    @TableField("directory_id")
    private Long directoryId;

    /**
     * 文件名，对应 {@code name}。
     */
    @TableField("name")
    private String name;

    /**
     * 文件大小，单位字节，对应 {@code size}。
     */
    @TableField("size")
    private Long size;

    /**
     * 文件 MIME 类型，对应 {@code mime_type}。
     */
    @TableField("mime_type")
    private String mimeType;

    /**
     * 文件扩展名，对应 {@code extension}。
     */
    @TableField("extension")
    private String extension;

    /**
     * 内部存储位置，对应 {@code storage_key}。
     */
    @TableField("storage_key")
    private String storageKey;

    /**
     * 上传人用户标识，对应 {@code uploaded_by}。
     */
    @TableField("uploaded_by")
    private Long uploadedBy;

    /**
     * 同名版本组标识，对应 {@code version_group_id}。
     */
    @TableField("version_group_id")
    private String versionGroupId;

    /**
     * 版本号，对应 {@code version_no}。
     */
    @TableField("version_no")
    private Integer versionNo;

    /**
     * 文件状态，对应 {@code status}。
     */
    @TableField("status")
    private String status;

    /**
     * 删除时间，对应 {@code deleted_at}。
     */
    @TableField("deleted_at")
    private LocalDateTime deletedAt;

    /**
     * 删除人用户标识，对应 {@code deleted_by}。
     */
    @TableField("deleted_by")
    private Long deletedBy;

    /**
     * 获取文件标识。
     *
     * @return 文件标识
     */
    public String getId() {
        return id;
    }

    /**
     * 设置文件标识。
     *
     * @param id 文件标识
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * 获取所属项目标识。
     *
     * @return 所属项目标识
     */
    public Long getProjectId() {
        return projectId;
    }

    /**
     * 设置所属项目标识。
     *
     * @param projectId 所属项目标识
     */
    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    /**
     * 获取所属目录标识。
     *
     * @return 所属目录标识
     */
    public Long getDirectoryId() {
        return directoryId;
    }

    /**
     * 设置所属目录标识。
     *
     * @param directoryId 所属目录标识
     */
    public void setDirectoryId(Long directoryId) {
        this.directoryId = directoryId;
    }

    /**
     * 获取文件名。
     *
     * @return 文件名
     */
    public String getName() {
        return name;
    }

    /**
     * 设置文件名。
     *
     * @param name 文件名
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取文件大小。
     *
     * @return 文件大小，单位字节
     */
    public Long getSize() {
        return size;
    }

    /**
     * 设置文件大小。
     *
     * @param size 文件大小，单位字节
     */
    public void setSize(Long size) {
        this.size = size;
    }

    /**
     * 获取文件 MIME 类型。
     *
     * @return 文件 MIME 类型
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * 设置文件 MIME 类型。
     *
     * @param mimeType 文件 MIME 类型
     */
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * 获取文件扩展名。
     *
     * @return 文件扩展名
     */
    public String getExtension() {
        return extension;
    }

    /**
     * 设置文件扩展名。
     *
     * @param extension 文件扩展名
     */
    public void setExtension(String extension) {
        this.extension = extension;
    }

    /**
     * 获取内部存储位置。
     *
     * @return 内部存储位置
     */
    public String getStorageKey() {
        return storageKey;
    }

    /**
     * 设置内部存储位置。
     *
     * @param storageKey 内部存储位置
     */
    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }

    /**
     * 获取上传人用户标识。
     *
     * @return 上传人用户标识
     */
    public Long getUploadedBy() {
        return uploadedBy;
    }

    /**
     * 设置上传人用户标识。
     *
     * @param uploadedBy 上传人用户标识
     */
    public void setUploadedBy(Long uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    /**
     * 获取同名版本组标识。
     *
     * @return 同名版本组标识
     */
    public String getVersionGroupId() {
        return versionGroupId;
    }

    /**
     * 设置同名版本组标识。
     *
     * @param versionGroupId 同名版本组标识
     */
    public void setVersionGroupId(String versionGroupId) {
        this.versionGroupId = versionGroupId;
    }

    /**
     * 获取版本号。
     *
     * @return 版本号
     */
    public Integer getVersionNo() {
        return versionNo;
    }

    /**
     * 设置版本号。
     *
     * @param versionNo 版本号
     */
    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
    }

    /**
     * 获取文件状态。
     *
     * @return 文件状态
     */
    public String getStatus() {
        return status;
    }

    /**
     * 设置文件状态。
     *
     * @param status 文件状态
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * 获取删除时间。
     *
     * @return 删除时间
     */
    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    /**
     * 设置删除时间。
     *
     * @param deletedAt 删除时间
     */
    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    /**
     * 获取删除人用户标识。
     *
     * @return 删除人用户标识
     */
    public Long getDeletedBy() {
        return deletedBy;
    }

    /**
     * 设置删除人用户标识。
     *
     * @param deletedBy 删除人用户标识
     */
    public void setDeletedBy(Long deletedBy) {
        this.deletedBy = deletedBy;
    }
}
