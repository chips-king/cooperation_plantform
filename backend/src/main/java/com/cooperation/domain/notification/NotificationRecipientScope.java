package com.cooperation.domain.notification;

/**
 * 通知接收范围枚举。
 */
public enum NotificationRecipientScope {

    /** 发送给相关目录有权限成员和负责人。 */
    RELATED_MEMBERS_AND_OWNER,

    /** 发送给全组成员。 */
    ALL_GROUP_MEMBERS,

    /** 发送给受影响成员和负责人。 */
    AFFECTED_MEMBERS_AND_OWNER
}
