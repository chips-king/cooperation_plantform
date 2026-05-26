package com.cooperation.web.group;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cooperation.application.group.CreateGroupUseCase;
import com.cooperation.application.group.GetGroupDetailUseCase;
import com.cooperation.application.group.ListGroupsUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * 小组 Web API 契约测试，限定表现层统一响应结构和小组资源字段。
 */
@WebMvcTest(controllers = GroupController.class)
@AutoConfigureMockMvc(addFilters = false)
class GroupControllerTest {

    /** 测试用操作用户标识，用请求头模拟后续认证上下文。 */
    private static final long ACTOR_ID = 1001L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CreateGroupUseCase createGroupUseCase;

    @MockBean
    private ListGroupsUseCase listGroupsUseCase;

    @MockBean
    private GetGroupDetailUseCase getGroupDetailUseCase;

    /**
     * 创建小组成功时应返回统一成功结构，并把小组标识放在 data 中。
     *
     * @throws Exception MockMvc 请求执行失败时抛出。
     */
    @Test
    @DisplayName("POST /groups 创建小组返回统一成功响应")
    void shouldCreateGroupWithUnifiedResponse() throws Exception {
        GroupDto.CreateGroupRequest request = new GroupDto.CreateGroupRequest("软件工程课程组");
        when(createGroupUseCase.create(any(CreateGroupUseCase.Command.class)))
                .thenReturn(new CreateGroupUseCase.Result(1L));

        mockMvc.perform(post("/groups")
                        .header("X-User-Id", ACTOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data.groupId").value(1L));
    }

    /**
     * 小组列表应使用统一响应包裹分页数据，为后续 PageResponse 契约留出位置。
     *
     * @throws Exception MockMvc 请求执行失败时抛出。
     */
    @Test
    @DisplayName("GET /groups 查询小组列表返回分页响应")
    void shouldListGroupsWithPageResponse() throws Exception {
        ListGroupsUseCase.Result result = new ListGroupsUseCase.Result(
                List.of(new GroupDto.GroupResponse(1L, "软件工程课程组", ACTOR_ID, "active")),
                1,
                20,
                1L
        );
        when(listGroupsUseCase.list(any(ListGroupsUseCase.Query.class))).thenReturn(result);

        mockMvc.perform(get("/groups")
                        .header("X-User-Id", ACTOR_ID)
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].id").value(1L))
                .andExpect(jsonPath("$.data.items[0].name").value("软件工程课程组"))
                .andExpect(jsonPath("$.data.items[0].status").value("active"))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.total").value(1L));
    }

    /**
     * 小组详情应返回资源详情字段，便于小组详情页展示基本信息。
     *
     * @throws Exception MockMvc 请求执行失败时抛出。
     */
    @Test
    @DisplayName("GET /groups/{groupId} 查询小组详情")
    void shouldGetGroupDetail() throws Exception {
        GroupDto.GroupResponse response = new GroupDto.GroupResponse(1L, "软件工程课程组", ACTOR_ID, "active");
        when(getGroupDetailUseCase.get(any(GetGroupDetailUseCase.Query.class))).thenReturn(response);

        mockMvc.perform(get("/groups/{groupId}", 1L)
                        .header("X-User-Id", ACTOR_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.name").value("软件工程课程组"))
                .andExpect(jsonPath("$.data.ownerId").value(ACTOR_ID))
                .andExpect(jsonPath("$.data.status").value("active"));
    }

    /**
     * 创建小组参数非法时应返回统一错误结构，字段错误列表由 W012-W013 落地。
     *
     * @throws Exception MockMvc 请求执行失败时抛出。
     */
    @Test
    @DisplayName("POST /groups 参数非法返回统一错误响应")
    void shouldReturnValidationErrorWhenCreateGroupRequestInvalid() throws Exception {
        GroupDto.CreateGroupRequest request = new GroupDto.CreateGroupRequest("");

        mockMvc.perform(post("/groups")
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
