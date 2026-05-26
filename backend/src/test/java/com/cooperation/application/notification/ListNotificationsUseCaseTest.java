package com.cooperation.application.notification;

import com.cooperation.domain.notification.NotificationEventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 通知列表用例测试。
 */
class ListNotificationsUseCaseTest {

    /**
     * 验证用户只能查询自己的通知并按时间倒序返回。
     */
    @Test
    @DisplayName("用户只能查看自己的通知列表")
    void shouldListCurrentUserNotificationsOnly() {
        FakeNotificationPort notificationPort = new FakeNotificationPort();
        ListNotificationsUseCase useCase = new ListNotificationsUseCase(notificationPort);

        ListNotificationsUseCase.Result result = useCase.list(new ListNotificationsUseCase.Query(
                100L,
                Optional.empty(),
                Optional.empty()
        ));

        assertEquals(List.of("notice-2", "notice-1"), result.notifications().stream().map(ListNotificationsUseCase.NotificationItem::id).toList());
    }

    /**
     * 验证通知列表支持按项目和已读状态筛选。
     */
    @Test
    @DisplayName("按项目和未读状态筛选通知")
    void shouldFilterNotificationsByProjectAndReadState() {
        FakeNotificationPort notificationPort = new FakeNotificationPort();
        ListNotificationsUseCase useCase = new ListNotificationsUseCase(notificationPort);

        ListNotificationsUseCase.Result result = useCase.list(new ListNotificationsUseCase.Query(
                100L,
                Optional.of("project-1"),
                Optional.of(false)
        ));

        assertEquals(List.of("notice-1"), result.notifications().stream().map(ListNotificationsUseCase.NotificationItem::id).toList());
        assertTrue(result.notifications().get(0).readAt().isEmpty());
    }

    /**
     * 验证用户可以把自己的通知标记为已读。
     */
    @Test
    @DisplayName("用户可以标记自己的通知为已读")
    void shouldMarkOwnNotificationAsRead() {
        FakeNotificationPort notificationPort = new FakeNotificationPort();
        ListNotificationsUseCase useCase = new ListNotificationsUseCase(notificationPort);
        Instant readAt = Instant.parse("2026-05-24T12:00:00Z");

        ListNotificationsUseCase.NotificationItem item = useCase.markRead(new ListNotificationsUseCase.MarkReadCommand(
                100L,
                "notice-1",
                readAt
        ));

        assertEquals(Optional.of(readAt), item.readAt());
        assertEquals(Optional.of(readAt), notificationPort.findById("notice-1").orElseThrow().readAt());
    }

    /**
     * 验证用户不能标记其他人的通知。
     */
    @Test
    @DisplayName("用户不能标记他人的通知为已读")
    void shouldRejectMarkingOtherUserNotificationAsRead() {
        FakeNotificationPort notificationPort = new FakeNotificationPort();
        ListNotificationsUseCase useCase = new ListNotificationsUseCase(notificationPort);

        assertThrows(AccessDeniedException.class, () -> useCase.markRead(new ListNotificationsUseCase.MarkReadCommand(
                100L,
                "notice-3",
                Instant.parse("2026-05-24T12:00:00Z")
        )));
    }

    /**
     * 通知内存假端口，表达通知查询和已读保存能力。
     */
    private static final class FakeNotificationPort implements ListNotificationsUseCase.NotificationPort {

        private final List<ListNotificationsUseCase.NotificationItem> notifications = new ArrayList<>(List.of(
                item("notice-1", "project-1", 100L, NotificationEventType.FILE_UPLOADED, "文件已上传", Optional.empty(), "2026-05-24T08:00:00Z"),
                item("notice-2", "project-2", 100L, NotificationEventType.MAIL_SENT, "邮件已发送", Optional.of(Instant.parse("2026-05-24T09:00:00Z")), "2026-05-24T10:00:00Z"),
                item("notice-3", "project-1", 200L, NotificationEventType.PROJECT_ENDED, "项目已结束", Optional.empty(), "2026-05-24T11:00:00Z")
        ));

        @Override
        public List<ListNotificationsUseCase.NotificationItem> listByRecipient(Long recipientId, Optional<String> projectId, Optional<Boolean> read) {
            return notifications.stream()
                    .filter(notification -> notification.recipientId().equals(recipientId))
                    .filter(notification -> projectId.map(value -> value.equals(notification.projectId())).orElse(true))
                    .filter(notification -> read.map(value -> value == notification.readAt().isPresent()).orElse(true))
                    .sorted((left, right) -> right.createdAt().compareTo(left.createdAt()))
                    .toList();
        }

        @Override
        public Optional<ListNotificationsUseCase.NotificationItem> findById(String notificationId) {
            return notifications.stream().filter(notification -> notification.id().equals(notificationId)).findFirst();
        }

        @Override
        public ListNotificationsUseCase.NotificationItem save(ListNotificationsUseCase.NotificationItem notification) {
            notifications.removeIf(item -> item.id().equals(notification.id()));
            notifications.add(notification);
            return notification;
        }

        /**
         * 创建通知列表项。
         */
        private static ListNotificationsUseCase.NotificationItem item(
                String id,
                String projectId,
                Long recipientId,
                NotificationEventType type,
                String title,
                Optional<Instant> readAt,
                String createdAt
        ) {
            return new ListNotificationsUseCase.NotificationItem(
                    id,
                    projectId,
                    recipientId,
                    type,
                    title,
                    title,
                    readAt,
                    Instant.parse(createdAt)
            );
        }
    }
}
