package com.cooperation.application.notification;

import com.cooperation.domain.notification.NotificationEventType;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 用户通知查询用例。
 */
public class QueryNotificationUseCase {

    private final NotificationRepository repository;

    /**
     * 创建用户通知查询用例。
     *
     * @param repository 通知仓储端口
     */
    public QueryNotificationUseCase(NotificationRepository repository) {
        this.repository = Objects.requireNonNull(repository, "通知仓储不能为空");
    }

    /**
     * 查询当前用户通知列表。
     *
     * @param query 通知查询条件
     * @return 通知查询结果
     */
    public Result list(Query query) {
        Objects.requireNonNull(query, "通知查询条件不能为空");
        return new Result(repository.findByRecipient(query.recipientId(), query.projectId(), query.read()));
    }

    /**
     * 将当前用户自己的通知标记为已读。
     *
     * @param command 标记已读命令
     * @return 标记后的通知项
     */
    public NotificationItem markRead(MarkReadCommand command) {
        Objects.requireNonNull(command, "通知已读命令不能为空");
        NotificationItem notification = repository.findById(command.notificationId())
                .orElseThrow(() -> new IllegalArgumentException("通知不存在"));
        if (!notification.recipientId().equals(command.recipientId())) {
            throw new AccessDeniedException("只能操作自己的通知");
        }
        return repository.save(notification.markRead(command.readAt()));
    }

    /**
     * 通知查询与保存仓储端口。
     */
    public interface NotificationRepository {

        /**
         * 查询接收人的通知。
         *
         * @param recipientId 接收人用户标识
         * @param projectId 项目筛选条件
         * @param read 已读状态筛选
         * @return 通知项列表
         */
        List<NotificationItem> findByRecipient(Long recipientId, Optional<String> projectId, Optional<Boolean> read);

        /**
         * 按通知标识查询通知。
         *
         * @param notificationId 通知标识
         * @return 匹配的通知项
         */
        Optional<NotificationItem> findById(String notificationId);

        /**
         * 保存通知项。
         *
         * @param notification 通知项
         * @return 保存后的通知项
         */
        NotificationItem save(NotificationItem notification);
    }

    /**
     * 通知列表查询条件。
     *
     * @param recipientId 接收人用户标识
     * @param projectId 项目筛选条件
     * @param read 已读状态筛选
     */
    public record Query(Long recipientId, Optional<String> projectId, Optional<Boolean> read) {

        /**
         * 规范化通知查询条件。
         */
        public Query {
            Objects.requireNonNull(recipientId, "接收人标识不能为空");
            projectId = Objects.requireNonNullElse(projectId, Optional.empty());
            read = Objects.requireNonNullElse(read, Optional.empty());
        }
    }

    /**
     * 通知标记已读命令。
     *
     * @param recipientId 当前用户标识
     * @param notificationId 通知标识
     * @param readAt 阅读时间
     */
    public record MarkReadCommand(Long recipientId, String notificationId, Instant readAt) {

        /**
         * 规范化通知标记已读命令。
         */
        public MarkReadCommand {
            Objects.requireNonNull(recipientId, "接收人标识不能为空");
            Objects.requireNonNull(notificationId, "通知标识不能为空");
            Objects.requireNonNull(readAt, "阅读时间不能为空");
        }
    }

    /**
     * 通知查询结果。
     *
     * @param notifications 通知项列表
     */
    public record Result(List<NotificationItem> notifications) {

        /**
         * 规范化通知查询结果。
         */
        public Result {
            notifications = List.copyOf(Objects.requireNonNull(notifications, "通知列表不能为空"));
        }
    }

    /**
     * 通知列表项。
     *
     * @param id 通知标识
     * @param projectId 项目标识
     * @param recipientId 接收人用户标识
     * @param type 通知事件类型
     * @param title 通知标题
     * @param content 通知内容
     * @param readAt 阅读时间
     * @param createdAt 创建时间
     */
    public record NotificationItem(
            String id,
            String projectId,
            Long recipientId,
            NotificationEventType type,
            String title,
            String content,
            Optional<Instant> readAt,
            Instant createdAt
    ) {

        /**
         * 规范化通知列表项。
         */
        public NotificationItem {
            Objects.requireNonNull(id, "通知标识不能为空");
            Objects.requireNonNull(projectId, "项目标识不能为空");
            Objects.requireNonNull(recipientId, "接收人标识不能为空");
            Objects.requireNonNull(type, "通知事件类型不能为空");
            Objects.requireNonNull(title, "通知标题不能为空");
            Objects.requireNonNull(content, "通知内容不能为空");
            readAt = Objects.requireNonNullElse(readAt, Optional.empty());
            Objects.requireNonNull(createdAt, "创建时间不能为空");
        }

        /**
         * 返回已读后的通知项副本。
         *
         * @param readAt 阅读时间
         * @return 已读通知项
         */
        public NotificationItem markRead(Instant readAt) {
            return new NotificationItem(id, projectId, recipientId, type, title, content, Optional.of(readAt), createdAt);
        }
    }
}
