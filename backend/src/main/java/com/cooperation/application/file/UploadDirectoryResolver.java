package com.cooperation.application.file;

/**
 * 上传目录解析端口，用于根据浏览器提供的相对路径定位或创建目标目录。
 */
public interface UploadDirectoryResolver {

    /**
     * 解析上传文件最终应落入的目录。
     *
     * @param projectId 项目标识
     * @param baseDirectoryId 用户当前选中的目录标识
     * @param relativePath 浏览器提供的相对路径，普通文件可为空
     * @param actorId 当前上传用户标识
     * @return 最终目标目录标识
     */
    String resolveTargetDirectory(String projectId, String baseDirectoryId, String relativePath, String actorId);
}
