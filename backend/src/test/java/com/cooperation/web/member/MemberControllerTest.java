package com.cooperation.web.member;

import com.cooperation.application.member.CreateInvitationUseCase;
import com.cooperation.application.member.JoinByInvitationUseCase;
import com.cooperation.application.member.QueryInvitationUseCase;
import com.cooperation.application.member.RemoveMemberUseCase;
import com.cooperation.application.member.ReviewJoinRequestUseCase;
import com.cooperation.domain.permission.RoleTemplate;
import com.cooperation.web.member.MemberDto.ApproveJoinRequestResponse;
import com.cooperation.web.member.MemberDto.CreateInvitationResponse;
import com.cooperation.web.member.MemberDto.InvitationDetailResponse;
import com.cooperation.web.member.MemberDto.JoinInvitationResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 成员与邀请接口测试，约束邀请创建、详情、加入和审核接口的 Web 契约。
 */
@WebMvcTest(MemberController.class)
@AutoConfigureMockMvc(addFilters = false)
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreateInvitationUseCase createInvitationUseCase;

    @MockBean
    private QueryInvitationUseCase queryInvitationUseCase;

    @MockBean
    private JoinByInvitationUseCase joinByInvitationUseCase;

    @MockBean
    private ReviewJoinRequestUseCase reviewJoinRequestUseCase;

    @MockBean
    private RemoveMemberUseCase removeMemberUseCase;

    /**
     * 创建直接加入邀请时，应返回统一成功结构和邀请链接信息。
     *
     * @throws Exception MockMvc 调用失败时抛出异常。
     */
    @Test
    void createDirectJoinInvitationReturnsUnifiedResponse() throws Exception {
        when(createInvitationUseCase.create(any()))
                .thenReturn(new CreateInvitationResponse(
                        71L,
                        21L,
                        501L,
                        "direct",
                        "DIRECT-CODE",
                        "/invitations/DIRECT-CODE"
                ));

        mockMvc.perform(post("/groups/{groupId}/invitations", 21L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectId": 501,
                                  "mode": "direct",
                                  "roleTemplate": "MEMBER"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data.invitationId").value(71))
                .andExpect(jsonPath("$.data.groupId").value(21))
                .andExpect(jsonPath("$.data.projectId").value(501))
                .andExpect(jsonPath("$.data.mode").value("direct"))
                .andExpect(jsonPath("$.data.code").value("DIRECT-CODE"))
                .andExpect(jsonPath("$.data.invitationUrl").value("/invitations/DIRECT-CODE"));
    }

    /**
     * 查询邀请详情时，应返回邀请状态、模式和目标项目信息。
     *
     * @throws Exception MockMvc 调用失败时抛出异常。
     */
    @Test
    void getInvitationDetailReturnsUnifiedResponse() throws Exception {
        when(queryInvitationUseCase.detail("REVIEW-CODE"))
                .thenReturn(new InvitationDetailResponse(
                        72L,
                        21L,
                        "课程设计小组",
                        501L,
                        "期末资料整理",
                        "review",
                        "valid"
                ));

        mockMvc.perform(get("/invitations/{code}", "REVIEW-CODE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data.invitationId").value(72))
                .andExpect(jsonPath("$.data.groupName").value("课程设计小组"))
                .andExpect(jsonPath("$.data.projectName").value("期末资料整理"))
                .andExpect(jsonPath("$.data.mode").value("review"))
                .andExpect(jsonPath("$.data.status").value("valid"));
    }

    /**
     * 直接加入邀请时，应返回 joined 状态并给出新成员关系标识。
     *
     * @throws Exception MockMvc 调用失败时抛出异常。
     */
    @Test
    void joinDirectInvitationReturnsJoinedStatus() throws Exception {
        when(joinByInvitationUseCase.join(any()))
                .thenReturn(new JoinByInvitationUseCase.Result(JoinByInvitationUseCase.JoinStatus.JOINED));

        mockMvc.perform(post("/invitations/{code}/join", "DIRECT-CODE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": 1002
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data.status").value("joined"));
    }

    /**
     * 需要审核的邀请加入时，应返回 pending_review 状态而非正式加入。
     *
     * @throws Exception MockMvc 调用失败时抛出异常。
     */
    @Test
    void joinReviewInvitationReturnsPendingReviewStatus() throws Exception {
        when(joinByInvitationUseCase.join(any()))
                .thenReturn(new JoinByInvitationUseCase.Result(JoinByInvitationUseCase.JoinStatus.PENDING_REVIEW));

        mockMvc.perform(post("/invitations/{code}/join", "REVIEW-CODE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": 1003
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data.status").value("pending_review"));
    }

    /**
     * 审核同意加入申请时，应返回审核结果和生效角色模板。
     *
     * @throws Exception MockMvc 调用失败时抛出异常。
     */
    @Test
    void approveJoinRequestReturnsApprovedStatus() throws Exception {
        when(reviewJoinRequestUseCase.approve(any()))
                .thenReturn(new ApproveJoinRequestResponse(
                        301L,
                        1003L,
                        21L,
                        501L,
                        RoleTemplate.MEMBER,
                        "approved"
                ));

        mockMvc.perform(post("/join-requests/{requestId}/approve", 301L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "operatorId": 1001,
                                  "roleTemplate": "MEMBER"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data.requestId").value(301))
                .andExpect(jsonPath("$.data.userId").value(1003))
                .andExpect(jsonPath("$.data.roleTemplate").value("MEMBER"))
                .andExpect(jsonPath("$.data.status").value("approved"));
    }

    /**
     * 审核拒绝加入申请时，应返回 rejected 状态和拒绝原因。
     *
     * @throws Exception MockMvc 调用失败时抛出异常。
     */
    @Test
    void rejectJoinRequestReturnsRejectedStatus() throws Exception {
        when(reviewJoinRequestUseCase.reject(any()))
                .thenReturn(new JoinInvitationResponse(
                        "rejected",
                        "申请信息与项目不匹配"
                ));

        mockMvc.perform(post("/join-requests/{requestId}/reject", 302L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "operatorId": 1001,
                                  "reason": "申请信息与项目不匹配"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data.status").value("rejected"))
                .andExpect(jsonPath("$.data.message").value("申请信息与项目不匹配"));
    }

    /**
     * 移除成员时，应把当前用户和成员关系标识交给应用用例进行权限校验。
     *
     * @throws Exception MockMvc 调用失败时抛出异常。
     */
    @Test
    void removeMemberDelegatesToUseCaseWithCurrentUser() throws Exception {
        mockMvc.perform(delete("/memberships/{membershipId}", 201L)
                        .header("X-User-Id", 1001L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("操作成功"));

        verify(removeMemberUseCase).remove(new RemoveMemberUseCase.Command(1001L, 201L));
    }
}
