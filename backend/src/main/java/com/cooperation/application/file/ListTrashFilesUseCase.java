package com.cooperation.application.file;

import com.cooperation.web.file.FileDto.TrashFileResponse;

import java.util.List;

/**
 * 查询项目回收站文件用例端口。
 */
public interface ListTrashFilesUseCase {

    /**
     * 查询指定项目的回收站文件。
     *
     * @param projectId 项目标识。
     * @return 回收站文件列表。
     */
    List<TrashFileResponse> listTrash(String projectId);
}
