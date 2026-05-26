package com.cooperation.application.file;

import com.cooperation.web.file.FileDto.DownloadResponse;

/**
 * 下载文件用例端口，供 Web 层获取附件元数据和二进制内容。
 */
public interface DownloadFileUseCase {

    /**
     * 下载指定文件。
     *
     * @param fileId 文件标识。
     * @return 文件下载响应数据。
     */
    DownloadResponse download(String fileId);
}
