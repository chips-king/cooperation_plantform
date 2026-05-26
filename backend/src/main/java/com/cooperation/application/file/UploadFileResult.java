package com.cooperation.application.file;

import com.cooperation.domain.file.FileAsset;

/**
 * 上传文件结果，返回新创建的文件元数据和文件类型信息。
 *
 * @param file 上传后成为当前结果的文件资产。
 * @param archive 是否为压缩包文件。
 */
public record UploadFileResult(FileAsset file, boolean archive) {
}
