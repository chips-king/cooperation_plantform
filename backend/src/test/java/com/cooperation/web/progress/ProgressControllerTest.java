package com.cooperation.web.progress;

import com.cooperation.application.directory.ListProjectProgressUseCase;
import com.cooperation.application.directory.UpdateDirectoryStatusResult;
import com.cooperation.application.directory.UpdateDirectoryStatusUseCase;
import com.cooperation.domain.directory.DirectoryNode;
import com.cooperation.domain.directory.DirectoryStatus;
import com.cooperation.web.progress.ProgressDto.DirectoryProgressResponse;
import com.cooperation.web.progress.ProgressDto.ProjectProgressResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 项目进度 Web API 契约测试，只覆盖目录三态进度的查询与更新。
 */
@WebMvcTest(ProgressController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProgressControllerTest {

    /** 测试项目标识，用于进度查询接口。 */
    private static final String PROJECT_ID = "project-001";

    /** 测试目录标识，用于目录状态更新接口。 */
    private static final String DIRECTORY_ID = "directory-source";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ListProjectProgressUseCase listProjectProgressUseCase;

    @MockBean
    private UpdateDirectoryStatusUseCase updateDirectoryStatusUseCase;

    @Test
    @DisplayName("GET /projects/{projectId}/progress 返回目录进度列表")
    void shouldReturnProjectProgress() throws Exception {
        // 进度响应包含三态枚举值和中文展示名，便于前端直接渲染任务进度。
        ProjectProgressResponse response = new ProjectProgressResponse(
                PROJECT_ID,
                3,
                1,
                List.of(
                        new DirectoryProgressResponse("directory-prepare", "资料准备", "completed", "已完成", Instant.parse("2026-05-24T09:00:00Z"), 2, false),
                        new DirectoryProgressResponse(DIRECTORY_ID, "源代码", "in_progress", "进行中", Instant.parse("2026-05-24T10:00:00Z"), 5, false),
                        new DirectoryProgressResponse("directory-doc", "说明文档", "not_started", "未开始", Instant.parse("2026-05-24T11:00:00Z"), 0, false)
                )
        );
        given(listProjectProgressUseCase.getProgress(PROJECT_ID)).willReturn(response);

        mockMvc.perform(get("/projects/{projectId}/progress", PROJECT_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data.projectId").value(PROJECT_ID))
                .andExpect(jsonPath("$.data.totalDirectoryCount").value(3))
                .andExpect(jsonPath("$.data.completedDirectoryCount").value(1))
                .andExpect(jsonPath("$.data.directories[0].status").value("completed"))
                .andExpect(jsonPath("$.data.directories[1].status").value("in_progress"))
                .andExpect(jsonPath("$.data.directories[2].status").value("not_started"));
    }

    @Test
    @DisplayName("PATCH /directories/{directoryId}/status 更新目录状态")
    void shouldUpdateDirectoryStatus() throws Exception {
        given(updateDirectoryStatusUseCase.update(any())).willReturn(new UpdateDirectoryStatusResult(completedDirectory()));

        mockMvc.perform(patch("/directories/{directoryId}/status", DIRECTORY_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectId": "project-001",
                                  "status": "completed"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.directoryId").value(DIRECTORY_ID))
                .andExpect(jsonPath("$.data.status").value("completed"))
                .andExpect(jsonPath("$.data.statusDisplayName").value("已完成"));

        then(updateDirectoryStatusUseCase).should().update(any());
    }

    @Test
    @DisplayName("PATCH /directories/{directoryId}/status 拒绝三态之外的目录状态")
    void shouldRejectUnsupportedDirectoryStatus() throws Exception {
        given(updateDirectoryStatusUseCase.update(any())).willThrow(new IllegalArgumentException("目录状态仅允许 not_started、in_progress、completed"));

        mockMvc.perform(patch("/directories/{directoryId}/status", DIRECTORY_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectId": "project-001",
                                  "status": "blocked"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("目录状态仅允许 not_started、in_progress、completed"));
    }

    /**
     * 创建已完成状态目录实体，用于模拟应用层状态更新结果。
     *
     * @return 已完成状态的目录实体
     */
    private DirectoryNode completedDirectory() {
        DirectoryNode directory = DirectoryNode.create(1L, null, "源代码", 1L);
        directory.changeStatus(DirectoryStatus.IN_PROGRESS, 1L);
        directory.changeStatus(DirectoryStatus.COMPLETED, 1L);
        return directory;
    }
}
