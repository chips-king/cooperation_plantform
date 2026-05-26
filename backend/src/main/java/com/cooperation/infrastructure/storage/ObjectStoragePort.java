package com.cooperation.infrastructure.storage;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * 对象存储端口，定义文件内容在对象存储中的保存、读取、删除和回收站移动能力。
 *
 * <p>本端口只描述基础对象操作契约，不绑定 Spring、具体云厂商 SDK 或本地文件系统实现。</p>
 */
public interface ObjectStoragePort {

    /**
     * 保存对象内容。
     *
     * @param command 保存对象命令，包含对象键、展示名、内容类型、内容长度和内容流
     * @return 已保存对象的基础描述
     * @throws IOException 当底层存储写入失败时抛出
     */
    StoredObject save(SaveObjectCommand command) throws IOException;

    /**
     * 读取对象内容。
     *
     * @param objectKey 对象键，不能为空
     * @return 对象内容流及其基础元数据
     * @throws IOException 当底层存储读取失败或对象不存在时抛出
     */
    ObjectContent read(String objectKey) throws IOException;

    /**
     * 永久删除对象。
     *
     * @param objectKey 对象键，不能为空
     * @throws IOException 当底层存储删除失败时抛出
     */
    void delete(String objectKey) throws IOException;

    /**
     * 将对象移动到回收站位置。
     *
     * @param objectKey 原对象键，不能为空
     * @param trashObjectKey 回收站对象键，不能为空
     * @return 移动后的对象基础描述
     * @throws IOException 当底层存储移动失败时抛出
     */
    StoredObject moveToTrash(String objectKey, String trashObjectKey) throws IOException;

    /**
     * 保存对象命令。
     *
     * @param objectKey 对象键，用于在存储中定位对象
     * @param filename 文件展示名，用于保留用户可读名称
     * @param contentType 内容类型；为空时按 {@code application/octet-stream} 处理
     * @param contentLength 内容长度，必须大于等于 0
     * @param content 对象内容流，调用方负责在调用后关闭
     */
    record SaveObjectCommand(
            String objectKey,
            String filename,
            String contentType,
            long contentLength,
            InputStream content
    ) {

        /**
         * 校验保存对象命令。
         */
        public SaveObjectCommand {
            objectKey = requireText(objectKey, "对象键不能为空");
            filename = requireText(filename, "文件展示名不能为空");
            contentType = normalizeContentType(contentType);
            if (contentLength < 0) {
                throw new IllegalArgumentException("内容长度不能小于 0");
            }
            content = Objects.requireNonNull(content, "对象内容流不能为空");
        }
    }

    /**
     * 已保存对象的基础描述。
     *
     * @param objectKey 对象键，用于在存储中定位对象
     * @param filename 文件展示名
     * @param contentType 内容类型
     * @param contentLength 内容长度；取值为 {@code -1} 表示底层存储未提供
     */
    record StoredObject(String objectKey, String filename, String contentType, long contentLength) {

        /**
         * 校验已保存对象描述。
         */
        public StoredObject {
            objectKey = requireText(objectKey, "对象键不能为空");
            filename = requireText(filename, "文件展示名不能为空");
            contentType = normalizeContentType(contentType);
            if (contentLength < -1) {
                throw new IllegalArgumentException("内容长度不能小于 -1");
            }
        }
    }

    /**
     * 对象内容读取结果。
     *
     * @param objectKey 对象键，用于在存储中定位对象
     * @param contentType 内容类型
     * @param contentLength 内容长度；取值为 {@code -1} 表示底层存储未提供
     * @param content 对象内容流，调用方负责关闭
     */
    record ObjectContent(String objectKey, String contentType, long contentLength, InputStream content) {

        /**
         * 校验对象内容读取结果。
         */
        public ObjectContent {
            objectKey = requireText(objectKey, "对象键不能为空");
            contentType = normalizeContentType(contentType);
            if (contentLength < -1) {
                throw new IllegalArgumentException("内容长度不能小于 -1");
            }
            content = Objects.requireNonNull(content, "对象内容流不能为空");
        }
    }

    /**
     * 校验并规整必填文本。
     *
     * @param value 待校验文本
     * @param message 校验失败提示
     * @return 去除首尾空白后的文本
     */
    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    /**
     * 规整内容类型。
     *
     * @param contentType 原始内容类型
     * @return 非空内容类型
     */
    private static String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return "application/octet-stream";
        }
        return contentType.trim();
    }
}
