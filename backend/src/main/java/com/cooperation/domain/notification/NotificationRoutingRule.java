package com.cooperation.domain.notification;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * 通知路由规则。
 */
public class NotificationRoutingRule {

    private final Map<NotificationEventType, NotificationRecipientScope> scopes;

    private NotificationRoutingRule(Map<NotificationEventType, NotificationRecipientScope> scopes) {
        this.scopes = new EnumMap<>(scopes);
    }

    /**
     * 创建系统默认通知路由规则。
     *
     * @return 默认通知路由规则
     */
    public static NotificationRoutingRule defaultRule() {
        Map<NotificationEventType, NotificationRecipientScope> scopes = new EnumMap<>(NotificationEventType.class);
        scopes.put(NotificationEventType.FILE_UPLOADED, NotificationRecipientScope.RELATED_MEMBERS_AND_OWNER);
        scopes.put(NotificationEventType.FILE_MOVED, NotificationRecipientScope.RELATED_MEMBERS_AND_OWNER);
        scopes.put(NotificationEventType.FILE_RENAMED, NotificationRecipientScope.RELATED_MEMBERS_AND_OWNER);
        scopes.put(NotificationEventType.FILE_DELETED, NotificationRecipientScope.RELATED_MEMBERS_AND_OWNER);
        scopes.put(NotificationEventType.FILE_RESTORED, NotificationRecipientScope.RELATED_MEMBERS_AND_OWNER);
        scopes.put(NotificationEventType.DIRECTORY_STATUS_CHANGED, NotificationRecipientScope.RELATED_MEMBERS_AND_OWNER);
        scopes.put(NotificationEventType.PACKAGE_CREATED, NotificationRecipientScope.ALL_GROUP_MEMBERS);
        scopes.put(NotificationEventType.MAIL_DRAFT_CREATED, NotificationRecipientScope.ALL_GROUP_MEMBERS);
        scopes.put(NotificationEventType.MAIL_SENT, NotificationRecipientScope.ALL_GROUP_MEMBERS);
        scopes.put(NotificationEventType.PROJECT_ENDED, NotificationRecipientScope.ALL_GROUP_MEMBERS);
        scopes.put(NotificationEventType.PROJECT_REOPENED, NotificationRecipientScope.ALL_GROUP_MEMBERS);
        scopes.put(NotificationEventType.MEMBER_PERMISSION_CHANGED, NotificationRecipientScope.AFFECTED_MEMBERS_AND_OWNER);
        return new NotificationRoutingRule(scopes);
    }

    /**
     * 解析通知事件对应的接收范围。
     *
     * @param eventType 通知事件类型
     * @return 通知接收范围
     */
    public NotificationRecipientScope resolve(NotificationEventType eventType) {
        NotificationRecipientScope scope = scopes.get(Objects.requireNonNull(eventType, "通知事件类型不能为空"));
        if (scope == null) {
            throw new IllegalArgumentException("未配置通知事件接收范围");
        }
        return scope;
    }
}
