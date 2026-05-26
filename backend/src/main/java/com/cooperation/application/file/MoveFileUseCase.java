package com.cooperation.application.file;

import com.cooperation.web.file.FileDto.FileItemResponse;
import com.cooperation.web.file.FileDto.MoveFileRequest;

/**
 * 移动文件用例端口，供 Web 层将文件移动到目标目录。
 */
public interface MoveFileUseCase {

    /**
     * 移动指定文件。
     *
     * @param fileId 文件标识。
     * @param request 移动文件请求。
     * @return 移动后的文件摘要。
     */
    FileItemResponse move(String fileId, MoveFileRequest request);
}
