package com.cooperation.web.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 登录 Web API 契约测试，确保前端登录页调用的接口真实存在。
 */
@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 管理员账号密码正确时，应返回前端会话需要的用户、令牌和权限摘要。
     *
     * @throws Exception MockMvc 请求执行失败时抛出。
     */
    @Test
    @DisplayName("POST /auth/login 管理员登录成功")
    void shouldLoginWithDefaultAdministrator() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "account", "admin",
                                "password", "123456"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.id").value(1L))
                .andExpect(jsonPath("$.data.user.displayName").value("管理员"))
                .andExpect(jsonPath("$.data.user.email").value("admin@example.com"))
                .andExpect(jsonPath("$.data.token").value("dev-token-1"))
                .andExpect(jsonPath("$.data.permissions").isArray());
    }

    /**
     * 管理员账号密码错误时，应返回未授权状态，避免前端误写入登录态。
     *
     * @throws Exception MockMvc 请求执行失败时抛出。
     */
    @Test
    @DisplayName("POST /auth/login 密码错误返回 401")
    void shouldRejectInvalidPassword() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "account", "admin",
                                "password", "wrong-password"
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_FAILED"));
    }
}
