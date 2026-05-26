package com.cooperation.domain.mail;

/**
 * 邮件草稿状态枚举。
 */
public enum MailDraftStatus {

    /** 草稿待确认或待发送。 */
    DRAFT,

    /** 草稿已经发送成功。 */
    SENT,

    /** 草稿已被取消，当前阶段预留。 */
    CANCELLED
}
