package com.cooperation.web.search;

import com.cooperation.application.search.SearchUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 搜索 Web 接口测试。
 */
@WebMvcTest(controllers = SearchController.class)
@AutoConfigureMockMvc(addFilters = false)
class SearchControllerTest {

    /** 当前测试用户标识，用于验证搜索按当前用户访问范围执行。 */
    private static final long CURRENT_USER_ID = 100L;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SearchUseCase searchUseCase;

    /**
     * 验证项目搜索接口只返回当前用户可见项目并使用统一响应结构。
     *
     * @throws Exception MockMvc 请求执行失败时抛出
     */
    @Test
    @DisplayName("按关键字搜索项目")
    @WithMockUser(username = "100")
    void shouldSearchProjects() throws Exception {
        when(searchUseCase.execute(any())).thenReturn(new SearchUseCase.Result(
                List.of(new SearchUseCase.ProjectHit(1L, 10L, "课设文档")),
                List.of(),
                List.of()
        ));

        mockMvc.perform(get("/search/projects").param("keyword", "课设"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data.projects[0].projectId").value(1))
                .andExpect(jsonPath("$.data.projects[0].groupId").value(10))
                .andExpect(jsonPath("$.data.projects[0].projectName").value("课设文档"))
                .andExpect(jsonPath("$.data.files.length()").value(0))
                .andExpect(jsonPath("$.data.members.length()").value(0));

        assertSearchQuery("课设");
    }

    /**
     * 验证文件搜索接口返回当前用户可访问的文件命中。
     *
     * @throws Exception MockMvc 请求执行失败时抛出
     */
    @Test
    @DisplayName("按关键字搜索文件")
    @WithMockUser(username = "100")
    void shouldSearchFiles() throws Exception {
        when(searchUseCase.execute(any())).thenReturn(new SearchUseCase.Result(
                List.of(),
                List.of(new SearchUseCase.FileHit("file-1", 1L, "结题报告.docx")),
                List.of()
        ));

        mockMvc.perform(get("/search/files").param("keyword", "报告"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.projects.length()").value(0))
                .andExpect(jsonPath("$.data.files[0].fileId").value("file-1"))
                .andExpect(jsonPath("$.data.files[0].projectId").value(1))
                .andExpect(jsonPath("$.data.files[0].fileName").value("结题报告.docx"))
                .andExpect(jsonPath("$.data.members.length()").value(0));

        assertSearchQuery("报告");
    }

    /**
     * 验证成员搜索接口返回共同项目范围内的成员命中。
     *
     * @throws Exception MockMvc 请求执行失败时抛出
     */
    @Test
    @DisplayName("按关键字搜索成员")
    @WithMockUser(username = "100")
    void shouldSearchMembers() throws Exception {
        when(searchUseCase.execute(any())).thenReturn(new SearchUseCase.Result(
                List.of(),
                List.of(),
                List.of(new SearchUseCase.MemberHit(201L, "小林"))
        ));

        mockMvc.perform(get("/search/members").param("keyword", "小林"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.projects.length()").value(0))
                .andExpect(jsonPath("$.data.files.length()").value(0))
                .andExpect(jsonPath("$.data.members[0].userId").value(201))
                .andExpect(jsonPath("$.data.members[0].displayName").value("小林"));

        assertSearchQuery("小林");
    }

    /**
     * 验证缺少搜索关键字时返回统一参数错误响应。
     *
     * @throws Exception MockMvc 请求执行失败时抛出
     */
    @Test
    @DisplayName("缺少搜索关键字返回参数错误")
    @WithMockUser(username = "100")
    void shouldReturnValidationErrorWhenKeywordMissing() throws Exception {
        mockMvc.perform(get("/search/projects"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    /**
     * 断言搜索请求会把认证用户和关键字传入应用层。
     *
     * @param expectedKeyword 期望传入应用层的搜索关键字
     */
    private void assertSearchQuery(String expectedKeyword) {
        ArgumentCaptor<SearchUseCase.Query> queryCaptor = ArgumentCaptor.forClass(SearchUseCase.Query.class);
        verify(searchUseCase).execute(queryCaptor.capture());
        assertEquals(CURRENT_USER_ID, queryCaptor.getValue().userId());
        assertEquals(expectedKeyword, queryCaptor.getValue().keyword());
    }
}
