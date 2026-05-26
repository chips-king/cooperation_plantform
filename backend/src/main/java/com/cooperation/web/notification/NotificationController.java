package com.cooperation.web.notification;

import com.cooperation.application.notification.ListNotificationsUseCase;
import com.cooperation.web.common.ApiResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 通知 Web API 控制器，负责通知列表查询和已读状态更新。
 */
@RestController
public class NotificationController {

    private final ListNotificationsUseCase listNotificationsUseCase;

    /**
     * 创建通知控制器实例。
     *
     * @param listNotificationsUseCase 通知列表与已读更新用例。
     */
    public NotificationController(ListNotificationsUseCase listNotificationsUseCase) {
        this.listNotificationsUseCase = listNotificationsUseCase;
    }

    /**
     * 查询当前用户的通知列表。
     *
     * @param headerUserId 请求头中的当前用户标识。
     * @param projectId 项目标识筛选。
     * @param read 已读状态筛选。
     * @return 统一通知列表响应。
     */
    @GetMapping("/notifications")
    public ApiResponse<NotificationListResponse> list(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestParam(required = false) String projectId,
            @RequestParam(required = false) Boolean read
    ) {
        ListNotificationsUseCase.Query query = new ListNotificationsUseCase.Query(
                currentUserId(headerUserId),
                Optional.ofNullable(projectId).filter(value -> !value.isBlank()),
                Optional.ofNullable(read)
        );
        ListNotificationsUseCase.Result result = listNotificationsUseCase.list(query);
        return ApiResponse.success(new NotificationListResponse(toResponses(result.notifications())));
    }

    /**
     * 将当前用户自己的通知标记为已读。
     *
     * @param headerUserId 请求头中的当前用户标识。
     * @param notificationId 通知标识。
     * @return 统一通知详情响应。
     */
    @PostMapping("/notifications/{notificationId}/read")
    public ApiResponse<NotificationResponse> markRead(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @PathVariable String notificationId
    ) {
        ListNotificationsUseCase.MarkReadCommand command = new ListNotificationsUseCase.MarkReadCommand(
                currentUserId(headerUserId),
                notificationId,
                Instant.now()
        );
        return ApiResponse.success(toResponse(listNotificationsUseCase.markRead(command)));
    }

    private Long currentUserId(Long headerUserId) {
        if (headerUserId != null) {
            return headerUserId;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalArgumentException("当前用户标识不能为空");
        }
        return Long.valueOf(authentication.getName());
    }

    private List<NotificationResponse> toResponses(List<ListNotificationsUseCase.NotificationItem> notifications) {
        return notifications.stream()
                .map(this::toResponse)
                .toList();
    }

    private NotificationResponse toResponse(ListNotificationsUseCase.NotificationItem notification) {
        return new NotificationResponse(
                notification.id(),
                notification.projectId(),
                notification.recipientId(),
                notification.type().name(),
                notification.title(),
                notification.content(),
                notification.readAt().orElse(null),
                notification.createdAt()
        );
    }

    /**
     * 通知列表响应。
     *
     * @param notifications 通知响应列表。
     */
    public record NotificationListResponse(List<NotificationResponse> notifications) {
    }

    /**
     * 通知详情响应。
     *
     * @param id 通知标识。
     * @param projectId 项目标识。
     * @param recipientId 接收人用户标识。
     * @param type 通知事件类型。
     * @param title 通知标题。
     * @param content 通知内容。
     * @param readAt 阅读时间，未读时为空。
     * @param createdAt 创建时间。
     */
    public record NotificationResponse(
            String id,
            String projectId,
            Long recipientId,
            String type,
            String title,
            String content,
            Instant readAt,
            Instant createdAt
    ) {
    }
}
