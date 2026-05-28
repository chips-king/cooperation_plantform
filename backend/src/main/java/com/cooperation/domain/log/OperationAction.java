package com.cooperation.domain.log;

/**
 * 操作记录动作类型枚举。
 */
public enum OperationAction {

    /** 小组被创建。 */
    GROUP_CREATED,

    /** 项目被创建。 */
    PROJECT_CREATED,

    /** 成员加入小组或项目。 */
    MEMBER_JOINED,

    /** 成员加入申请被审核。 */
    MEMBER_REVIEWED,

    /** 成员权限发生变更。 */
    PERMISSION_UPDATED,

    /** 文件上传。 */
    FILE_UPLOAD,

    /** 文件下载。 */
    FILE_DOWNLOAD,

    /** 文件移动。 */
    FILE_MOVE,

    /** 文件重命名。 */
    FILE_RENAME,

    /** 文件删除到回收站。 */
    FILE_DELETE,

    /** 文件从回收站恢复。 */
    FILE_RESTORE,

    /** 目录状态变更。 */
    DIRECTORY_STATUS_UPDATE,

    /** 执行打包前检查。 */
    CHECK_RUN,

    /** 应用清理建议。 */
    CLEANUP_APPLIED,

    /** 创建最终压缩包。 */
    PACKAGE_CREATED,

    /** 下载最终压缩包。 */
    PACKAGE_DOWNLOADED,

    /** 创建邮件草稿。 */
    MAIL_DRAFT_CREATED,

    /** 删除邮件草稿。 */
    MAIL_DRAFT_DELETED,

    /** 邮件发送成功。 */
    MAIL_SENT,

    /** 项目结束。 */
    PROJECT_ENDED,

    /** 项目重新打开。 */
    PROJECT_REOPENED,

    /** 项目被删除。 */
    PROJECT_DELETED
}
