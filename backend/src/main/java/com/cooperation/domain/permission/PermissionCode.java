package com.cooperation.domain.permission;

/**
 * 权限点枚举，覆盖项目协作中的资源查看、文件协作、打包、邮件和项目状态动作。
 */
public enum PermissionCode {

    /** 查看项目基础信息和进度。 */
    PROJECT_VIEW("project.view"),

    /** 管理项目基础信息。 */
    PROJECT_MANAGE("project.manage"),

    /** 管理项目成员。 */
    MEMBER_MANAGE("member.manage"),

    /** 管理成员权限配置。 */
    PERMISSION_MANAGE("permission.manage"),

    /** 查看项目文件列表。 */
    FILE_VIEW("file.view"),

    /** 上传项目文件。 */
    FILE_UPLOAD("file.upload"),

    /** 下载项目文件。 */
    FILE_DOWNLOAD("file.download"),

    /** 移动项目文件。 */
    FILE_MOVE("file.move"),

    /** 重命名项目文件。 */
    FILE_RENAME("file.rename"),

    /** 删除项目文件到回收站。 */
    FILE_DELETE("file.delete"),

    /** 从回收站恢复项目文件。 */
    FILE_RESTORE("file.restore"),

    /** 管理项目目录结构。 */
    DIRECTORY_MANAGE("directory.manage"),

    /** 更新目录任务状态。 */
    DIRECTORY_STATUS_UPDATE("directory.status.update"),

    /** 执行打包前检查。 */
    CHECK_RUN("check.run"),

    /** 应用清理建议。 */
    CLEANUP_APPLY("cleanup.apply"),

    /** 创建最终压缩包。 */
    PACKAGE_CREATE("package.create"),

    /** 下载最终压缩包。 */
    PACKAGE_DOWNLOAD("package.download"),

    /** 创建邮件草稿。 */
    MAIL_DRAFT_CREATE("mail.draft.create"),

    /** 更新邮件草稿。 */
    MAIL_DRAFT_UPDATE("mail.draft.update"),

    /** 发送邮件。 */
    MAIL_SEND("mail.send"),

    /** 查看操作记录。 */
    LOG_VIEW("log.view"),

    /** 查看平台内通知。 */
    NOTIFICATION_VIEW("notification.view"),

    /** 结束项目协作。 */
    PROJECT_END("project.end"),

    /** 重新打开已结束项目。 */
    PROJECT_REOPEN("project.reopen");

    private final String code;

    PermissionCode(String code) {
        this.code = code;
    }

    /**
     * 获取持久化和接口传输使用的权限编码。
     *
     * @return 权限编码字符串。
     */
    public String code() {
        return code;
    }
}
