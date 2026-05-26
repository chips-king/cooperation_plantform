package com.cooperation.application.file;

import com.cooperation.domain.file.FileAsset;

/**
 * 删除文件结果，返回进入回收站后的文件资产。
 *
 * @param file 已更新状态的文件资产。
 */
public record DeleteFileResult(FileAsset file) {
}
