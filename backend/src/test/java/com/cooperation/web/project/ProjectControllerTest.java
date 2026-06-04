package com.cooperation.web.project;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cooperation.application.project.CreateProjectUseCase;
import com.cooperation.application.project.DeleteProjectUseCase;
import com.cooperation.application.project.EndProjectUseCase;
import com.cooperation.application.project.GetProjectDetailUseCase;
import com.cooperation.application.project.ReopenProjectUseCase;
import com.cooperation.domain.project.Project;
import com.cooperation.domain.project.ProjectRepository;
import com.cooperation.domain.project.ProjectStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 项目 Web API 契约测试，限定创建、详情、结束和重新打开接口行为。
 */
@WebMvcTest(controllers = ProjectController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProjectControllerTest {

    /** 测试用操作用户标识，用请求头模拟后续认证上下文。 */
    private static final long ACTOR_ID = 1001L;

    /** 测试用小组标识，表达项目必须从小组下创建。 */
    private static final long GROUP_ID = 10L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CreateProjectUseCase createProjectUseCase;

    @MockBean
    private GetProjectDetailUseCase getProjectDetailUseCase;

    @MockBean
    private EndProjectUseCase endProjectUseCase;

    @MockBean
    private ReopenProjectUseCase reopenProjectUseCase;

    @MockBean
    private DeleteProjectUseCase deleteProjectUseCase;

    @MockBean
    private ProjectRepository projectRepository;

    /**
     * 创建项目成功时应返回项目标识、状态和更新时间。
     *
     * @throws Exception MockMvc 请求执行失败时抛出。
     */
    @Test
    @DisplayName("POST /groups/{groupId}/projects 创建项目")
    void shouldCreateProjectUnderGroup() throws Exception {
        Instant updatedAt = Instant.parse("2026-05-24T10:30:00Z");
        ProjectDto.CreateProjectRequest request = new ProjectDto.CreateProjectRequest("课程资料整理");
        when(createProjectUseCase.create(any(CreateProjectUseCase.Command.class)))
                .thenReturn(new CreateProjectUseCase.Result(20L, ProjectStatus.ACTIVE, updatedAt));

        mockMvc.perform(post("/groups/{groupId}/projects", GROUP_ID)
                        .header("X-User-Id", ACTOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data.projectId").value(20L))
                .andExpect(jsonPath("$.data.status").value("active"))
                .andExpect(jsonPath("$.data.updatedAt").value("2026-05-24T10:30:00Z"));
    }

    /**
     * 查询项目详情应返回项目所属小组、负责人和状态等核心字段。
     *
     * @throws Exception MockMvc 请求执行失败时抛出。
     */
    @Test
    @DisplayName("GET /projects/{projectId} 查询项目详情")
    void shouldGetProjectDetail() throws Exception {
        ProjectDto.ProjectDetailResponse response = new ProjectDto.ProjectDetailResponse(
                20L,
                GROUP_ID,
                "课程资料整理",
                ACTOR_ID,
                "active",
                null,
                null
        );
        when(getProjectDetailUseCase.get(any(GetProjectDetailUseCase.Query.class))).thenReturn(response);

        mockMvc.perform(get("/projects/{projectId}", 20L)
                        .header("X-User-Id", ACTOR_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(20L))
                .andExpect(jsonPath("$.data.groupId").value(GROUP_ID))
                .andExpect(jsonPath("$.data.name").value("课程资料整理"))
                .andExpect(jsonPath("$.data.ownerId").value(ACTOR_ID))
                .andExpect(jsonPath("$.data.status").value("active"));
    }

    /**
     * 查询项目列表应返回当前用户最近参与项目，供前端首页项目列表调用。
     *
     * @throws Exception MockMvc 请求执行失败时抛出。
     */
    @Test
    @DisplayName("GET /projects 查询最近项目列表")
    void shouldListRecentProjects() throws Exception {
        Project project = Project.restore(20L, GROUP_ID, ACTOR_ID, "课程资料整理", ProjectStatus.ACTIVE);
        when(projectRepository.findRecentByUserId(ACTOR_ID, 20)).thenReturn(List.of(project));

        mockMvc.perform(get("/projects")
                        .header("X-User-Id", ACTOR_ID)
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].id").value(20L))
                .andExpect(jsonPath("$.data.items[0].groupId").value(GROUP_ID))
                .andExpect(jsonPath("$.data.items[0].name").value("课程资料整理"))
                .andExpect(jsonPath("$.data.items[0].ownerId").value(ACTOR_ID))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    /**
     * 结束项目应返回已结束状态，为项目写操作锁定和通知实现保留契约。
     *
     * @throws Exception MockMvc 请求执行失败时抛出。
     */
    @Test
    @DisplayName("POST /projects/{projectId}/end 结束项目")
    void shouldEndProject() throws Exception {
        Project endedProject = Project.restore(20L, GROUP_ID, ACTOR_ID, "课程资料整理", ProjectStatus.ACTIVE);
        endedProject.end(ACTOR_ID);
        when(endProjectUseCase.handle(any(EndProjectUseCase.Command.class)))
                .thenReturn(new EndProjectUseCase.Result(endedProject));

        mockMvc.perform(post("/projects/{projectId}/end", 20L)
                        .header("X-User-Id", ACTOR_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(20L))
                .andExpect(jsonPath("$.data.status").value("ended"))
                .andExpect(jsonPath("$.data.endedAt").isNotEmpty());
    }

    /**
     * 重新打开项目应返回协作中状态，为恢复写操作和通知实现保留契约。
     *
     * @throws Exception MockMvc 请求执行失败时抛出。
     */
    @Test
    @DisplayName("POST /projects/{projectId}/reopen 重新打开项目")
    void shouldReopenProject() throws Exception {
        Project reopenedProject = Project.restore(20L, GROUP_ID, ACTOR_ID, "课程资料整理", ProjectStatus.ACTIVE);
        when(reopenProjectUseCase.handle(any(ReopenProjectUseCase.Command.class)))
                .thenReturn(new ReopenProjectUseCase.Result(reopenedProject));

        mockMvc.perform(post("/projects/{projectId}/reopen", 20L)
                        .header("X-User-Id", ACTOR_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(20L))
                .andExpect(jsonPath("$.data.status").value("active"));
    }

    /**
     * 创建项目参数非法时应返回统一错误结构，供后续全局异常处理器实现。
     *
     * @throws Exception MockMvc 请求执行失败时抛出。
     */
    @Test
    @DisplayName("POST /groups/{groupId}/projects 参数非法返回统一错误响应")
    void shouldReturnValidationErrorWhenCreateProjectRequestInvalid() throws Exception {
        ProjectDto.CreateProjectRequest request = new ProjectDto.CreateProjectRequest("");

        mockMvc.perform(post("/groups/{groupId}/projects", GROUP_ID)
                        .header("X-User-Id", ACTOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }
}
