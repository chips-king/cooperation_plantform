package com.cooperation.web.mail;

import com.cooperation.application.mail.SmtpConfigRepository;
import com.cooperation.domain.mail.SmtpConfig;
import com.cooperation.infrastructure.mail.SmtpPasswordEncryptor;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SMTP 配置接口测试，约束编辑配置时密码留空的兼容行为。
 */
@WebMvcTest(SmtpConfigController.class)
@AutoConfigureMockMvc(addFilters = false)
class SmtpConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SmtpConfigRepository smtpConfigRepository;

    @MockBean
    private SmtpPasswordEncryptor passwordEncryptor;

    @Test
    void updateConfigPreservesExistingPasswordWhenRequestPasswordBlank() throws Exception {
        SmtpConfig existing = SmtpConfig.create(
                "旧配置", "smtp.example.com", 465, "user", "encrypted-old",
                "from@example.com", "imap.example.com", 993, true, false, true, 1001L
        ).withId(9L);
        when(smtpConfigRepository.findById(9L)).thenReturn(Optional.of(existing));
        when(smtpConfigRepository.save(org.mockito.ArgumentMatchers.any(SmtpConfig.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(put("/smtp-configs/{id}", 9L)
                        .header("X-User-Id", 1001L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "新配置",
                                  "host": "smtp.example.com",
                                  "port": 465,
                                  "username": "user",
                                  "password": "",
                                  "fromAddress": "from@example.com",
                                  "imapHost": "imap.example.com",
                                  "imapPort": 993,
                                  "sslEnabled": true,
                                  "starttlsEnabled": false,
                                  "isDefault": true
                                }
                                """))
                .andExpect(status().isOk());

        ArgumentCaptor<SmtpConfig> configCaptor = ArgumentCaptor.forClass(SmtpConfig.class);
        verify(smtpConfigRepository).save(configCaptor.capture());
        verify(passwordEncryptor, never()).encrypt(anyString());
        assertThat(configCaptor.getValue().getPassword()).isEqualTo("encrypted-old");
    }
}
