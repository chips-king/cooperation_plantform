package com.cooperation.application.notification;

import com.cooperation.domain.notification.NotificationEventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 通知查询用例测试。
 */
class QueryNotificationUseCaseTest {

    /**
     * 验证用户只能查询发送给自己的通知。
     */
    @Test
    @DisplayName("用户只能查询自己的通知列表")
    void shouldListNotificationsForCurrentUserOnly() {
        FakeNotificationRepository repository = new FakeNotificationRepository();
        QueryNotificationUseCase useCase = new QueryNotificationUseCase(repository);

        QueryNotificationUseCase.Result result = useCase.list(new QueryNotificationUseCase.Query(100L, Optional.empty(), Optional.empty()));

        assertEquals(List.of("notification-2", "notification-1"), result.notifications().stream().map(QueryNotificationUseCase.NotificationItem::id).toList());
    }

    /**
     * 验证通知列表支持按项目和未读状态筛选。
     */
    @Test
    @DisplayName("支持按项目和未读状态筛选通知")
    void shouldFilterNotificationsByProjectAndUnreadStatus() {
        FakeNotificationRepository repository = new FakeNotificationRepository();
        QueryNotificationUseCase useCase = new QueryNotificationUseCase(repository);

        QueryNotificationUseCase.Result result = useCase.list(new QueryNotificationUseCase.Query(100L, Optional.of("project-1"), Optional.of(false)));

        assertEquals(List.of("notification-1"), result.notifications().stream().map(QueryNotificationUseCase.NotificationItem::id).toList());
        assertTrue(result.notifications().get(0).readAt().isEmpty());
    }

    /**
     * 验证标记已读会写入阅读时间并保持只能操作自己的通知。
     */
    @Test
    @DisplayName("用户可以将自己的通知标记为已读")
    void shouldMarkOwnNotificationAsRead() {
        FakeNotificationRepository repository = new FakeNotificationRepository();
        QueryNotificationUseCase useCase = new QueryNotificationUseCase(repository);
        Instant readAt = Instant.parse("2026-05-24T12:00:00Z");

        QueryNotificationUseCase.NotificationItem item = useCase.markRead(new QueryNotificationUseCase.MarkReadCommand(100L, "notification-1", readAt));

        assertEquals(Optional.of(readAt), item.readAt());
        assertEquals(Optional.of(readAt), repository.findById("notification-1").orElseThrow().readAt());
    }

    /**
     * 通知测试内存仓储，表达应用层通知查询和已读更新端口。
     */
    private static final class FakeNotificationRepository implements QueryNotificationUseCase.NotificationRepository {

        private final List<QueryNotificationUseCase.NotificationItem> notifications = new ArrayList<>(List.of(
                item("notification-1", "project-1", 100L, NotificationEventType.FILE_UPLOADED, "报告已上传", Optional.empty(), "2026-05-24T08:00:00Z"),
                item("notification-2", "project-2", 100L, NotificationEventType.MAIL_SENT, "邮件已发送", Optional.of(Instant.parse("2026-05-24T09:30:00Z")), "2026-05-24T10:00:00Z"),
                item("notification-3", "project-1", 200L, NotificationEventType.PROJECT_ENDED, "项目已结束", Optional.empty(), "2026-05-24T11:00:00Z")
        ));

        @Override
        public List<QueryNotificationUseCase.NotificationItem> findByRecipient(Long recipientId, Optional<String> projectId, Optional<Boolean> read) {
            return notifications.stream()
                    .filter(item -> item.recipientId().equals(recipientId))
                    .filter(item -> projectId.map(value -> value.equals(item.projectId())).orElse(true))
                    .filter(item -> read.map(value -> value == item.readAt().isPresent()).orElse(true))
                    .sorted((left, right) -> right.createdAt().compareTo(left.createdAt()))
                    .toList();
        }

        @Override
        public Optional<QueryNotificationUseCase.NotificationItem> findById(String notificationId) {
            return notifications.stream().filter(item -> item.id().equals(notificationId)).findFirst();
        }

        @Override
        public QueryNotificationUseCase.NotificationItem save(QueryNotificationUseCase.NotificationItem notification) {
            notifications.removeIf(item -> item.id().equals(notification.id()));
            notifications.add(notification);
            return notification;
        }

        private static QueryNotificationUseCase.NotificationItem item(
                String id,
                String projectId,
                Long recipientId,
                NotificationEventType type,
                String title,
                Optional<Instant> readAt,
                String createdAt
        ) {
            return new QueryNotificationUseCase.NotificationItem(
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
