package com.cooperation.domain.notification;

/**
 * 通知事件类型枚举。
 */
public enum NotificationEventType {

    /** 文件上传事件。 */
    FILE_UPLOADED,

    /** 文件移动事件。 */
    FILE_MOVED,

    /** 文件重命名事件。 */
    FILE_RENAMED,

    /** 文件删除事件。 */
    FILE_DELETED,

    /** 文件恢复事件。 */
    FILE_RESTORED,

    /** 目录状态变化事件。 */
    DIRECTORY_STATUS_CHANGED,

    /** 最终压缩包创建事件。 */
    PACKAGE_CREATED,

    /** 邮件草稿创建事件。 */
    MAIL_DRAFT_CREATED,

    /** 邮件发送事件。 */
    MAIL_SENT,

    /** 项目结束事件。 */
    PROJECT_ENDED,

    /** 项目重新打开事件。 */
    PROJECT_REOPENED,

    /** 成员或权限变更事件。 */
    MEMBER_PERMISSION_CHANGED
}
