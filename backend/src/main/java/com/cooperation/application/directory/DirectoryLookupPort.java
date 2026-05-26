package com.cooperation.application.directory;

/**
 * 目录存在性查询端口，用于应用层确认恢复目标目录是否可用。
 */
public interface DirectoryLookupPort {

    /**
     * 判断项目下目录是否存在。
     *
     * @param projectId 项目标识。
     * @param directoryId 目录标识。
     * @return 目录存在时返回 true。
     */
    boolean existsByProjectIdAndDirectoryId(String projectId, String directoryId);
}
