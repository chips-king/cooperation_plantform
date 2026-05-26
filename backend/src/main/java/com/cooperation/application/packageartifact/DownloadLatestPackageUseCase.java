package com.cooperation.application.packageartifact;

/**
 * 下载项目最近压缩包的应用层端口。
 */
public interface DownloadLatestPackageUseCase {

    /**
     * 下载指定项目对当前用户可见的最近压缩包。
     *
     * @param command 最近压缩包下载命令
     * @return 最近压缩包二进制内容
     */
    Result download(Command command);

    /**
     * 最近压缩包下载命令。
     *
     * @param projectId 项目标识
     * @param actorId 当前用户标识
     */
    record Command(String projectId, String actorId) {

        /**
         * 校验最近压缩包下载命令。
         */
        public Command {
            projectId = requireText(projectId, "项目标识不能为空");
            actorId = requireText(actorId, "当前用户不能为空");
        }
    }

    /**
     * 最近压缩包下载结果。
     *
     * @param filename 附件展示文件名
     * @param contentType 响应内容类型
     * @param content 压缩包二进制内容
     */
    record Result(String filename, String contentType, byte[] content) {

        /**
         * 校验最近压缩包下载结果。
         */
        public Result {
            filename = requireText(filename, "压缩包文件名不能为空");
            contentType = requireText(contentType, "响应内容类型不能为空");
            content = content == null ? new byte[0] : content.clone();
        }

        /**
         * 获取防御性复制后的二进制内容。
         *
         * @return 压缩包二进制内容
         */
        @Override
        public byte[] content() {
            return content.clone();
        }
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
