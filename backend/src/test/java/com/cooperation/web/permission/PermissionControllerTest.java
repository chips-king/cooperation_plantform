package com.cooperation.web.permission;

import com.cooperation.application.permission.QueryProjectPermissionUseCase;
import com.cooperation.application.permission.UpdateMemberPermissionUseCase;
import com.cooperation.domain.permission.PermissionCode;
import com.cooperation.domain.permission.RoleTemplate;
import com.cooperation.web.permission.PermissionDto.MemberPermissionResponse;
import com.cooperation.web.permission.PermissionDto.ProjectPermissionResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 权限接口测试，约束项目权限查询、成员权限更新和无权限错误结构。
 */
@WebMvcTest(PermissionController.class)
@AutoConfigureMockMvc(addFilters = false)
class PermissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QueryProjectPermissionUseCase queryProjectPermissionUseCase;

    @MockBean
    private UpdateMemberPermissionUseCase updateMemberPermissionUseCase;

    /**
     * 查询项目权限时，应返回成员权限列表和权限模板信息。
     *
     * @throws Exception MockMvc 调用失败时抛出异常。
     */
    @Test
    void getProjectPermissionsReturnsUnifiedResponse() throws Exception {
        when(queryProjectPermissionUseCase.query(any()))
                .thenReturn(new ProjectPermissionResponse(
                        501L,
                        List.of(
                                new MemberPermissionResponse(
                                        11L,
                                        1001L,
                                        "负责人",
                                        RoleTemplate.OWNER,
                                        orderedPermissions(PermissionCode.PROJECT_VIEW, PermissionCode.PERMISSION_MANAGE)
                                ),
                                new MemberPermissionResponse(
                                        12L,
                                        1002L,
                                        "成员甲",
                                        RoleTemplate.MEMBER,
                                        orderedPermissions(PermissionCode.PROJECT_VIEW, PermissionCode.FILE_UPLOAD)
                                )
                        )
                ));

        mockMvc.perform(get("/projects/{projectId}/permissions", 501L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data.projectId").value(501))
                .andExpect(jsonPath("$.data.members[0].membershipId").value(11))
                .andExpect(jsonPath("$.data.members[0].roleTemplate").value("OWNER"))
                .andExpect(jsonPath("$.data.members[0].permissions[0]").value("PROJECT_VIEW"))
                .andExpect(jsonPath("$.data.members[1].membershipId").value(12))
                .andExpect(jsonPath("$.data.members[1].roleTemplate").value("MEMBER"))
                .andExpect(jsonPath("$.data.members[1].permissions[1]").value("FILE_UPLOAD"));
    }

    /**
     * 更新成员权限时，应返回统一成功结构和更新后的权限集合。
     *
     * @throws Exception MockMvc 调用失败时抛出异常。
     */
    @Test
    void updateMemberPermissionReturnsUpdatedPermissions() throws Exception {
        when(updateMemberPermissionUseCase.update(any()))
                .thenReturn(new UpdateMemberPermissionUseCase.Result(
                        12L,
                        orderedPermissions(
                                PermissionCode.PROJECT_VIEW,
                                PermissionCode.FILE_UPLOAD,
                                PermissionCode.LOG_VIEW
                        )
                ));

        mockMvc.perform(patch("/memberships/{membershipId}/permissions", 12L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "operatorId": 1001,
                                  "permissions": [
                                    "PROJECT_VIEW",
                                    "FILE_UPLOAD",
                                    "LOG_VIEW"
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data.membershipId").value(12))
                .andExpect(jsonPath("$.data.permissions").isArray())
                .andExpect(jsonPath("$.data.permissions[0]").value("PROJECT_VIEW"))
                .andExpect(jsonPath("$.data.permissions[1]").value("FILE_UPLOAD"))
                .andExpect(jsonPath("$.data.permissions[2]").value("LOG_VIEW"));
    }

    /**
     * 无权限更新成员权限时，应返回 403 和统一错误结构。
     *
     * @throws Exception MockMvc 调用失败时抛出异常。
     */
    @Test
    void updateMemberPermissionWithoutPermissionReturnsForbiddenError() throws Exception {
        when(updateMemberPermissionUseCase.update(any()))
                .thenThrow(new AccessDeniedException("无权限更新成员权限"));

        mockMvc.perform(patch("/memberships/{membershipId}/permissions", 12L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "operatorId": 1002,
                                  "permissions": [
                                    "PROJECT_VIEW"
                                  ]
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("无权限更新成员权限"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    /**
     * 按声明顺序构造权限集合，避免 JSON 数组断言受 Set 默认顺序影响。
     *
     * @param permissions 权限枚举列表。
     * @return 保持插入顺序的权限集合。
     */
    private static Set<PermissionCode> orderedPermissions(PermissionCode... permissions) {
        return new LinkedHashSet<>(List.of(permissions));
    }
}
