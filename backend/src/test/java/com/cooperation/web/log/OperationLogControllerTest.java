package com.cooperation.web.log;

import com.cooperation.application.log.ListOperationLogsUseCase;
import com.cooperation.domain.log.OperationAction;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 操作记录 Web 接口测试。
 */
@WebMvcTest(controllers = OperationLogController.class)
@AutoConfigureMockMvc(addFilters = false)
class OperationLogControllerTest {

    /** 当前测试用户标识，用于验证 Controller 从认证信息传入用例。 */
    private static final long CURRENT_USER_ID = 100L;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ListOperationLogsUseCase listOperationLogsUseCase;

    /**
     * 验证成员查询项目操作记录时返回统一成功响应和记录列表。
     *
     * @throws Exception MockMvc 请求执行失败时抛出
     */
    @Test
    @DisplayName("成员可以查询项目全部操作记录")
    @WithMockUser(username = "100")
    void shouldListProjectOperationLogs() throws Exception {
        when(listOperationLogsUseCase.handle(any())).thenReturn(new ListOperationLogsUseCase.Result(List.of(
                new ListOperationLogsUseCase.LogItem(
                        "log-1",
                        "project-1",
                        100L,
                        OperationAction.FILE_UPLOAD,
                        "file",
                        "file-1",
                        "上传了结题报告",
                        Instant.parse("2026-05-24T08:00:00Z")
                )
        )));

        mockMvc.perform(get("/projects/{projectId}/operation-logs", "project-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data.logs[0].id").value("log-1"))
                .andExpect(jsonPath("$.data.logs[0].projectId").value("project-1"))
                .andExpect(jsonPath("$.data.logs[0].actorId").value(100))
                .andExpect(jsonPath("$.data.logs[0].action").value("FILE_UPLOAD"))
                .andExpect(jsonPath("$.data.logs[0].targetType").value("file"))
                .andExpect(jsonPath("$.data.logs[0].targetId").value("file-1"))
                .andExpect(jsonPath("$.data.logs[0].summary").value("上传了结题报告"))
                .andExpect(jsonPath("$.data.logs[0].createdAt").value("2026-05-24T08:00:00Z"));

        ArgumentCaptor<ListOperationLogsUseCase.Query> queryCaptor = ArgumentCaptor.forClass(ListOperationLogsUseCase.Query.class);
        verify(listOperationLogsUseCase).handle(queryCaptor.capture());
        assertEquals(CURRENT_USER_ID, queryCaptor.getValue().userId());
        assertEquals("project-1", queryCaptor.getValue().projectId());
    }

    /**
     * 验证动作、操作人和时间范围筛选会被转换为应用层查询条件。
     *
     * @throws Exception MockMvc 请求执行失败时抛出
     */
    @Test
    @DisplayName("操作记录支持按类型操作人和时间筛选")
    @WithMockUser(username = "100")
    void shouldFilterProjectOperationLogs() throws Exception {
        when(listOperationLogsUseCase.handle(any())).thenReturn(new ListOperationLogsUseCase.Result(List.of(
                new ListOperationLogsUseCase.LogItem(
                        "log-2",
                        "project-1",
                        200L,
                        OperationAction.FILE_DELETE,
                        "file",
                        "file-2",
                        "删除了旧文件",
                        Instant.parse("2026-05-24T09:00:00Z")
                )
        )));

        mockMvc.perform(get("/projects/{projectId}/operation-logs", "project-1")
                        .param("action", "FILE_DELETE")
                        .param("actorId", "200")
                        .param("from", "2026-05-24T08:00:00Z")
                        .param("to", "2026-05-24T10:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.logs.length()").value(1))
                .andExpect(jsonPath("$.data.logs[0].action").value("FILE_DELETE"))
                .andExpect(jsonPath("$.data.logs[0].actorId").value(200));

        ArgumentCaptor<ListOperationLogsUseCase.Query> queryCaptor = ArgumentCaptor.forClass(ListOperationLogsUseCase.Query.class);
        verify(listOperationLogsUseCase).handle(queryCaptor.capture());
        assertEquals(Optional.of(OperationAction.FILE_DELETE), queryCaptor.getValue().action());
        assertEquals(Optional.of(200L), queryCaptor.getValue().actorId());
        assertEquals(Optional.of(Instant.parse("2026-05-24T08:00:00Z")), queryCaptor.getValue().from());
        assertEquals(Optional.of(Instant.parse("2026-05-24T10:00:00Z")), queryCaptor.getValue().to());
    }

    /**
     * 验证只读用户被应用层拒绝时返回统一错误响应结构。
     *
     * @throws Exception MockMvc 请求执行失败时抛出
     */
    @Test
    @DisplayName("只读用户查询操作记录返回禁止访问错误")
    @WithMockUser(username = "100")
    void shouldReturnForbiddenWhenReadOnlyUserQueriesLogs() throws Exception {
        when(listOperationLogsUseCase.handle(any())).thenThrow(new AccessDeniedException("只读用户不可查看操作记录"));

        mockMvc.perform(get("/projects/{projectId}/operation-logs", "project-1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("只读用户不可查看操作记录"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}
