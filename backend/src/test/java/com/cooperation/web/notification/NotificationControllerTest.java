package com.cooperation.web.notification;

import com.cooperation.application.notification.ListNotificationsUseCase;
import com.cooperation.domain.notification.NotificationEventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 通知 Web 接口测试。
 */
@WebMvcTest(controllers = NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerTest {

    /** 当前测试用户标识，用于验证通知只查询当前接收人。 */
    private static final long CURRENT_USER_ID = 100L;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ListNotificationsUseCase listNotificationsUseCase;

    /**
     * 验证通知列表返回统一成功响应和当前用户通知数据。
     *
     * @throws Exception MockMvc 请求执行失败时抛出
     */
    @Test
    @DisplayName("用户可以查询自己的通知列表")
    @WithMockUser(username = "100")
    void shouldListCurrentUserNotifications() throws Exception {
        when(listNotificationsUseCase.list(any())).thenReturn(new ListNotificationsUseCase.Result(List.of(
                new ListNotificationsUseCase.NotificationItem(
                        "notification-1",
                        "project-1",
                        CURRENT_USER_ID,
                        NotificationEventType.FILE_UPLOADED,
                        "报告已上传",
                        "成员上传了结题报告",
                        Optional.empty(),
                        Instant.parse("2026-05-24T08:00:00Z")
                )
        )));

        mockMvc.perform(get("/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data.notifications[0].id").value("notification-1"))
                .andExpect(jsonPath("$.data.notifications[0].projectId").value("project-1"))
                .andExpect(jsonPath("$.data.notifications[0].recipientId").value(100))
                .andExpect(jsonPath("$.data.notifications[0].type").value("FILE_UPLOADED"))
                .andExpect(jsonPath("$.data.notifications[0].title").value("报告已上传"))
                .andExpect(jsonPath("$.data.notifications[0].content").value("成员上传了结题报告"))
                .andExpect(jsonPath("$.data.notifications[0].readAt").doesNotExist())
                .andExpect(jsonPath("$.data.notifications[0].createdAt").value("2026-05-24T08:00:00Z"));

        ArgumentCaptor<ListNotificationsUseCase.Query> queryCaptor = ArgumentCaptor.forClass(ListNotificationsUseCase.Query.class);
        verify(listNotificationsUseCase).list(queryCaptor.capture());
        assertEquals(CURRENT_USER_ID, queryCaptor.getValue().recipientId());
    }

    /**
     * 验证项目和已读状态筛选会被转换为通知查询条件。
     *
     * @throws Exception MockMvc 请求执行失败时抛出
     */
    @Test
    @DisplayName("通知列表支持按项目和未读状态筛选")
    @WithMockUser(username = "100")
    void shouldFilterNotificationsByProjectAndReadStatus() throws Exception {
        when(listNotificationsUseCase.list(any())).thenReturn(new ListNotificationsUseCase.Result(List.of(
                new ListNotificationsUseCase.NotificationItem(
                        "notification-2",
                        "project-1",
                        CURRENT_USER_ID,
                        NotificationEventType.MAIL_SENT,
                        "邮件已发送",
                        "负责人发送了邮件",
                        Optional.empty(),
                        Instant.parse("2026-05-24T09:00:00Z")
                )
        )));

        mockMvc.perform(get("/notifications")
                        .param("projectId", "project-1")
                        .param("read", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.notifications.length()").value(1))
                .andExpect(jsonPath("$.data.notifications[0].projectId").value("project-1"));

        ArgumentCaptor<ListNotificationsUseCase.Query> queryCaptor = ArgumentCaptor.forClass(ListNotificationsUseCase.Query.class);
        verify(listNotificationsUseCase).list(queryCaptor.capture());
        assertEquals(Optional.of("project-1"), queryCaptor.getValue().projectId());
        assertEquals(Optional.of(false), queryCaptor.getValue().read());
    }

    /**
     * 验证当前用户可以将自己的通知标记为已读。
     *
     * @throws Exception MockMvc 请求执行失败时抛出
     */
    @Test
    @DisplayName("用户可以标记自己的通知为已读")
    @WithMockUser(username = "100")
    void shouldMarkOwnNotificationAsRead() throws Exception {
        Instant readAt = Instant.parse("2026-05-24T10:00:00Z");
        when(listNotificationsUseCase.markRead(any())).thenReturn(new ListNotificationsUseCase.NotificationItem(
                "notification-1",
                "project-1",
                CURRENT_USER_ID,
                NotificationEventType.FILE_UPLOADED,
                "报告已上传",
                "成员上传了结题报告",
                Optional.of(readAt),
                Instant.parse("2026-05-24T08:00:00Z")
        ));

        mockMvc.perform(post("/notifications/{notificationId}/read", "notification-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("notification-1"))
                .andExpect(jsonPath("$.data.readAt").value("2026-05-24T10:00:00Z"));

        ArgumentCaptor<ListNotificationsUseCase.MarkReadCommand> commandCaptor =
                ArgumentCaptor.forClass(ListNotificationsUseCase.MarkReadCommand.class);
        verify(listNotificationsUseCase).markRead(commandCaptor.capture());
        assertEquals(CURRENT_USER_ID, commandCaptor.getValue().recipientId());
        assertEquals("notification-1", commandCaptor.getValue().notificationId());
        assertTrue(!commandCaptor.getValue().readAt().isBefore(Instant.parse("2026-05-24T00:00:00Z")));
    }

    /**
     * 验证尝试操作他人通知时返回统一禁止访问错误。
     *
     * @throws Exception MockMvc 请求执行失败时抛出
     */
    @Test
    @DisplayName("标记他人通知为已读返回禁止访问错误")
    @WithMockUser(username = "100")
    void shouldReturnForbiddenWhenMarkingOtherUserNotification() throws Exception {
        when(listNotificationsUseCase.markRead(any())).thenThrow(new AccessDeniedException("只能操作自己的通知"));

        mockMvc.perform(post("/notifications/{notificationId}/read", "notification-3"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("只能操作自己的通知"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}
