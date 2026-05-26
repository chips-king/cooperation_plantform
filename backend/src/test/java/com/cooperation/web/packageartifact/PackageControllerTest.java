package com.cooperation.web.packageartifact;

import com.cooperation.application.packageartifact.ApplyCleanupSuggestionUseCase;
import com.cooperation.application.packageartifact.CreatePackageUseCase;
import com.cooperation.application.packageartifact.DownloadLatestPackageUseCase;
import com.cooperation.application.packageartifact.QueryLatestPackageUseCase;
import com.cooperation.application.packageartifact.RunPackageCheckUseCase;
import com.cooperation.domain.check.CheckIssue;
import com.cooperation.domain.check.CheckIssueType;
import com.cooperation.domain.check.CheckReport;
import com.cooperation.domain.check.CleanupItem;
import com.cooperation.domain.check.CleanupSuggestion;
import com.cooperation.domain.packageartifact.PackageFormat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 检查、清理与最终打包 Web API 契约测试。
 */
@WebMvcTest(PackageController.class)
@AutoConfigureMockMvc(addFilters = false)
class PackageControllerTest {

    /** 项目标识用于表达项目级打包接口路径。 */
    private static final String PROJECT_ID = "project-001";

    /** 当前操作者用于表达 Controller 从安全上下文透传用户身份。 */
    private static final String ACTOR_ID = "user-owner";

    /** 打包快照时间用于校验压缩包响应必须暴露新契约字段。 */
    private static final Instant SNAPSHOT_CREATED_AT = Instant.parse("2026-05-24T12:30:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RunPackageCheckUseCase runPackageCheckUseCase;

    @MockBean
    private ApplyCleanupSuggestionUseCase applyCleanupSuggestionUseCase;

    @MockBean
    private CreatePackageUseCase createPackageUseCase;

    @MockBean
    private QueryLatestPackageUseCase queryLatestPackageUseCase;

    @MockBean
    private DownloadLatestPackageUseCase downloadLatestPackageUseCase;

    /**
     * 打包检查应返回统一成功结构，并暴露 canContinuePackaging 与清理建议。
     */
    @Test
    @DisplayName("打包检查返回可继续打包标记和清理建议")
    void shouldReturnPackageCheckReportWithCanContinuePackaging() throws Exception {
        given(runPackageCheckUseCase.run(any(RunPackageCheckUseCase.Command.class)))
                .willReturn(new RunPackageCheckUseCase.Result(
                        new CheckReport(List.of(CheckIssue.warning(CheckIssueType.CACHE_FILE, "src/__pycache__"))),
                        new CleanupSuggestion(List.of(new CleanupItem(CheckIssueType.CACHE_FILE, "src/__pycache__")))
                ));

        mockMvc.perform(post("/projects/{projectId}/checks", PROJECT_ID)
                        .header("X-User-Id", ACTOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data.canContinuePackaging").value(true))
                .andExpect(jsonPath("$.data.issues[0].type").value("CACHE_FILE"))
                .andExpect(jsonPath("$.data.issues[0].blocking").value(false))
                .andExpect(jsonPath("$.data.cleanupSuggestions[0].path").value("src/__pycache__"));
    }

    /**
     * 清理预览应只返回 previewObjects，不应表达任何已清理结果。
     */
    @Test
    @DisplayName("清理预览返回将处理对象列表")
    void shouldPreviewCleanupObjects() throws Exception {
        given(applyCleanupSuggestionUseCase.preview(any(ApplyCleanupSuggestionUseCase.PreviewCommand.class)))
                .willReturn(new ApplyCleanupSuggestionUseCase.PreviewResult(List.of(
                        new ApplyCleanupSuggestionUseCase.PreviewObject(
                                "src/debug.tmp",
                                "file-001",
                                "debug.tmp",
                                128L
                        )
                )));

        mockMvc.perform(post("/projects/{projectId}/cleanup-preview", PROJECT_ID)
                        .header("X-User-Id", ACTOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [
                                    {"path": "src/debug.tmp", "reason": "临时文件可清理"}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.data.previewObjects[0].objectId").value("file-001"))
                .andExpect(jsonPath("$.data.previewObjects[0].path").value("src/debug.tmp"))
                .andExpect(jsonPath("$.data.previewObjects[0].fileName").value("debug.tmp"))
                .andExpect(jsonPath("$.data.previewObjects[0].size").value(128L));
    }

    /**
     * 清理执行应返回统一成功结构，并只暴露已移入回收站的对象标识。
     */
    @Test
    @DisplayName("清理执行返回已清理对象")
    void shouldApplyCleanupSuggestions() throws Exception {
        given(applyCleanupSuggestionUseCase.apply(any(ApplyCleanupSuggestionUseCase.Command.class)))
                .willReturn(new ApplyCleanupSuggestionUseCase.Result(List.of("file-001", "file-002")));

        mockMvc.perform(post("/projects/{projectId}/cleanup", PROJECT_ID)
                        .header("X-User-Id", ACTOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [
                                    {"path": "src/debug.tmp", "reason": "临时文件可清理"},
                                    {"path": "Thumbs.db", "reason": "系统无关文件可清理"}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.data.cleanedObjectIds[0]").value("file-001"))
                .andExpect(jsonPath("$.data.cleanedObjectIds[1]").value("file-002"));
    }

    /**
     * 创建压缩包应返回快照时间 snapshotCreatedAt，确保前端和操作记录可对齐同一文件树快照。
     */
    @Test
    @DisplayName("创建压缩包返回快照时间和压缩包摘要")
    void shouldCreatePackageWithSnapshotCreatedAt() throws Exception {
        given(createPackageUseCase.create(any(CreatePackageUseCase.Command.class)))
                .willReturn(new CreatePackageUseCase.Result(
                        "package-001",
                        "final-report.zip",
                        PackageFormat.ZIP,
                        SNAPSHOT_CREATED_AT,
                        "packages/project-001/final-report.zip",
                        4096L
                ));

        mockMvc.perform(post("/projects/{projectId}/packages", PROJECT_ID)
                        .header("X-User-Id", ACTOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "baseName": "final-report",
                                  "format": "zip",
                                  "continueAfterCheck": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.data.packageId").value("package-001"))
                .andExpect(jsonPath("$.data.filename").value("final-report.zip"))
                .andExpect(jsonPath("$.data.format").value("zip"))
                .andExpect(jsonPath("$.data.snapshotCreatedAt").value(SNAPSHOT_CREATED_AT.toString()))
                .andExpect(jsonPath("$.data.size").value(4096L));
    }

    /**
     * 最近压缩包查询应返回统一成功结构，供前端进入下载前核对文件名。
     */
    @Test
    @DisplayName("查询最近压缩包返回最新压缩包摘要")
    void shouldReturnLatestPackageSummary() throws Exception {
        given(queryLatestPackageUseCase.query(any(QueryLatestPackageUseCase.Query.class)))
                .willReturn(new QueryLatestPackageUseCase.Result(
                        "package-001",
                        "final-report.zip",
                        "zip",
                        SNAPSHOT_CREATED_AT,
                        4096L
                ));

        mockMvc.perform(get("/projects/{projectId}/packages/latest", PROJECT_ID)
                        .header("X-User-Id", ACTOR_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.data.packageId").value("package-001"))
                .andExpect(jsonPath("$.data.filename").value("final-report.zip"))
                .andExpect(jsonPath("$.data.snapshotCreatedAt").value(SNAPSHOT_CREATED_AT.toString()));
    }

    /**
     * 最近压缩包下载应返回附件响应头，由 Controller 统一对接下载用例。
     */
    @Test
    @DisplayName("下载最近压缩包返回附件流")
    void shouldDownloadLatestPackage() throws Exception {
        given(downloadLatestPackageUseCase.download(any(DownloadLatestPackageUseCase.Command.class)))
                .willReturn(new DownloadLatestPackageUseCase.Result(
                        "final-report.zip",
                        MediaType.APPLICATION_OCTET_STREAM_VALUE,
                        new byte[]{1, 2, 3}
                ));

        mockMvc.perform(get("/projects/{projectId}/packages/latest/download", PROJECT_ID)
                        .header("X-User-Id", ACTOR_ID))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"final-report.zip\""))
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE));
    }

    /**
     * 参数校验失败应返回统一错误结构，避免前端为不同接口编写分散错误解析。
     */
    @Test
    @DisplayName("创建压缩包参数错误返回统一错误结构")
    void shouldReturnUnifiedErrorWhenPackageRequestInvalid() throws Exception {
        mockMvc.perform(post("/projects/{projectId}/packages", PROJECT_ID)
                        .header("X-User-Id", ACTOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "baseName": "",
                                  "format": "rar"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}
