package com.cooperation.application.file;

/**
 * 文件内容存储端口，应用层通过该端口保存和读取上传文件内容。
 */
public interface FileStoragePort {

    /**
     * 保存文件内容并返回内部存储键。
     *
     * @param projectId 项目标识。
     * @param directoryId 目录标识。
     * @param filename 文件展示名。
     * @param content 文件二进制内容。
     * @return 可用于后续定位文件内容的内部存储键。
     */
    String save(String projectId, String directoryId, String filename, byte[] content);

    /**
     * 根据存储键读取文件内容。
     *
     * @param storageKey 内部存储键。
     * @return 文件二进制内容。
     */
    byte[] load(String storageKey);
}
