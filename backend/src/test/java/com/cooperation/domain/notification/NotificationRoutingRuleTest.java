package com.cooperation.domain.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * 通知路由规则领域测试。
 */
class NotificationRoutingRuleTest {

    /**
     * 验证文件变化类通知只发送给相关成员和负责人。
     */
    @ParameterizedTest
    @EnumSource(value = NotificationEventType.class, names = {
            "FILE_UPLOADED",
            "FILE_MOVED",
            "FILE_RENAMED",
            "FILE_DELETED",
            "FILE_RESTORED"
    })
    void shouldRouteFileChangeEventsToRelatedMembers(NotificationEventType eventType) {
        NotificationRoutingRule rule = NotificationRoutingRule.defaultRule();

        assertEquals(NotificationRecipientScope.RELATED_MEMBERS_AND_OWNER, rule.resolve(eventType));
    }

    /**
     * 验证打包、邮件和项目结束通知发送给全组成员。
     */
    @ParameterizedTest
    @EnumSource(value = NotificationEventType.class, names = {
            "PACKAGE_CREATED",
            "MAIL_DRAFT_CREATED",
            "MAIL_SENT",
            "PROJECT_ENDED"
    })
    void shouldRoutePackageMailAndProjectEndEventsToWholeGroup(NotificationEventType eventType) {
        NotificationRoutingRule rule = NotificationRoutingRule.defaultRule();

        assertEquals(NotificationRecipientScope.ALL_GROUP_MEMBERS, rule.resolve(eventType));
    }
}
