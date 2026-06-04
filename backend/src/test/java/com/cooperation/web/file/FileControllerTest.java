package com.cooperation.web.file;

import com.cooperation.application.file.DeleteFileUseCase;
import com.cooperation.application.file.DirectoryManagementUseCase;
import com.cooperation.application.file.DownloadFileUseCase;
import com.cooperation.application.file.EmptyTrashUseCase;
import com.cooperation.application.file.ListDirectoryTreeUseCase;
import com.cooperation.application.file.ListTrashFilesUseCase;
import com.cooperation.application.file.MoveFileUseCase;
import com.cooperation.application.file.RestoreFileUseCase;
import com.cooperation.application.file.UploadDirectoryResolver;
import com.cooperation.application.file.UploadFileCommand;
import com.cooperation.application.file.DeleteFileResult;
import com.cooperation.application.file.RestoreFileResult;
import com.cooperation.application.file.UploadFileResult;
import com.cooperation.application.file.UploadFileUseCase;
import com.cooperation.domain.file.FileAsset;
import com.cooperation.domain.file.FileName;
import com.cooperation.web.file.FileDto.DirectoryTreeResponse;
import com.cooperation.web.file.FileDto.DownloadResponse;
import com.cooperation.web.file.FileDto.FileItemResponse;
import com.cooperation.web.file.FileDto.MoveFileRequest;
import com.cooperation.web.file.FileDto.TrashFileResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 文件与目录 Web API 契约测试，只校验表现层路由、请求参数和统一响应结构。
 */
@WebMvcTest(FileController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "app.file-storage.max-upload-bytes=1024")
class FileControllerTest {

    /** 测试项目标识，用于确保接口始终限制在项目维度。 */
    private static final String PROJECT_ID = "project-001";

    /** 测试目录标识，用于上传、移动和恢复目标目录。 */
    private static final String DIRECTORY_ID = "directory-source";

    /** 测试文件标识，用于下载、删除和恢复接口。 */
    private static final String FILE_ID = "file-001";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ListDirectoryTreeUseCase listDirectoryTreeUseCase;

    @MockBean
    private UploadFileUseCase uploadFileUseCase;

    @MockBean
    private UploadDirectoryResolver uploadDirectoryResolver;

    @MockBean
    private DownloadFileUseCase downloadFileUseCase;

    @MockBean
    private MoveFileUseCase moveFileUseCase;

    @MockBean
    private DeleteFileUseCase deleteFileUseCase;

    @MockBean
    private ListTrashFilesUseCase listTrashFilesUseCase;

    @MockBean
    private RestoreFileUseCase restoreFileUseCase;

    @MockBean
    private DirectoryManagementUseCase directoryManagementUseCase;

    @MockBean
    private EmptyTrashUseCase emptyTrashUseCase;

    @Test
    @DisplayName("GET /projects/{projectId}/tree 返回目录树和文件列表")
    void shouldReturnDirectoryTree() throws Exception {
        // 构造包含目录状态和文件摘要的树响应，约束前端文件管理页所需字段。
        DirectoryTreeResponse response = new DirectoryTreeResponse(
                PROJECT_ID,
                List.of(new DirectoryTreeResponse.DirectoryNodeResponse(
                        DIRECTORY_ID,
                        null,
                        "源代码",
                        "in_progress",
                        List.of(new FileItemResponse(FILE_ID, "README.md", 128L, "text/markdown", 1, "active", LocalDateTime.parse("2026-05-24T10:00:00"))),
                        List.of()
                ))
        );
        given(listDirectoryTreeUseCase.getTree(PROJECT_ID)).willReturn(response);

        mockMvc.perform(get("/projects/{projectId}/tree", PROJECT_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data.projectId").value(PROJECT_ID))
                .andExpect(jsonPath("$.data.directories[0].id").value(DIRECTORY_ID))
                .andExpect(jsonPath("$.data.directories[0].status").value("in_progress"))
                .andExpect(jsonPath("$.data.directories[0].files[0].name").value("README.md"));
    }

    @Test
    @DisplayName("POST /directories 创建当前目录下的分工目录")
    void shouldCreateDirectory() throws Exception {
        DirectoryTreeResponse.DirectoryNodeResponse response = new DirectoryTreeResponse.DirectoryNodeResponse(
                "directory-created",
                DIRECTORY_ID,
                "任务一",
                "in_progress",
                List.of(),
                List.of()
        );
        given(directoryManagementUseCase.create(any())).willReturn(response);

        mockMvc.perform(post("/directories")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectId": "project-001",
                                  "parentDirectoryId": "directory-source",
                                  "name": "任务一"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("directory-created"))
                .andExpect(jsonPath("$.data.parentId").value(DIRECTORY_ID))
                .andExpect(jsonPath("$.data.name").value("任务一"));

        verify(directoryManagementUseCase).create(new DirectoryManagementUseCase.CreateCommand(
                PROJECT_ID,
                DIRECTORY_ID,
                "任务一",
                "1"
        ));
    }

    @Test
    @DisplayName("DELETE /directories/{directoryId} 删除空目录后返回父目录标识")
    void shouldDeleteEmptyDirectory() throws Exception {
        given(directoryManagementUseCase.deleteEmpty(any())).willReturn(new DirectoryManagementUseCase.DeleteResult(DIRECTORY_ID));

        mockMvc.perform(delete("/directories/{directoryId}", "directory-created")
                        .header("X-User-Id", "1")
                        .param("projectId", PROJECT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.parentDirectoryId").value(DIRECTORY_ID));

        verify(directoryManagementUseCase).deleteEmpty(new DirectoryManagementUseCase.DeleteCommand(
                PROJECT_ID,
                "directory-created",
                "1"
        ));
    }

    @Test
    @DisplayName("POST /directories/{directoryId}/files 上传文件时传递同名处理策略")
    void shouldUploadFileWithDuplicatePolicy() throws Exception {
        // 上传请求必须显式表达 duplicatePolicy，便于 Controller 将同名策略传给应用层。
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "README.md",
                MediaType.TEXT_MARKDOWN_VALUE,
                "# 说明".getBytes()
        );
        given(uploadFileUseCase.upload(any())).willReturn(new UploadFileResult(activeFile(2), false));

        mockMvc.perform(multipart("/directories/{directoryId}/files", DIRECTORY_ID)
                        .file(file)
                        .param("projectId", PROJECT_ID)
                        .param("duplicatePolicy", "new_version"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fileId").value(FILE_ID))
                .andExpect(jsonPath("$.data.duplicatePolicy").value("new_version"))
                .andExpect(jsonPath("$.data.versionNo").value(2));

        then(uploadFileUseCase).should().upload(any());
    }

    @Test
    @DisplayName("POST /directories/{directoryId}/files 上传文件夹文件时按相对路径解析目标目录")
    void shouldResolveTargetDirectoryByRelativePathWhenUploadingFolderFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "App.vue",
                "text/x-vue",
                "<template />".getBytes()
        );
        given(uploadDirectoryResolver.resolveTargetDirectory(PROJECT_ID, DIRECTORY_ID, "src/views/App.vue", "1"))
                .willReturn("88");
        given(uploadFileUseCase.upload(any())).willReturn(new UploadFileResult(
                FileAsset.uploaded(FILE_ID, PROJECT_ID, "88", FileName.of("App.vue"), 128L, "text/x-vue", "storage/app", "1", LocalDateTime.now(), "version-group-001", 1),
                false
        ));

        mockMvc.perform(multipart("/directories/{directoryId}/files", DIRECTORY_ID)
                        .file(file)
                        .header("X-User-Id", "1")
                        .param("projectId", PROJECT_ID)
                        .param("relativePath", "src/views/App.vue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fileId").value(FILE_ID));

        verify(uploadDirectoryResolver).resolveTargetDirectory(PROJECT_ID, DIRECTORY_ID, "src/views/App.vue", "1");
        then(uploadFileUseCase).should().upload(any(UploadFileCommand.class));
    }

    @Test
    @DisplayName("POST /directories/{directoryId}/files 同名冲突返回处理选项")
    void shouldReturnDuplicateOptionsWhenUploadConflicts() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "README.md",
                MediaType.TEXT_MARKDOWN_VALUE,
                "# 冲突".getBytes()
        );
        given(uploadFileUseCase.upload(any())).willThrow(new IllegalStateException("存在同名文件，请选择处理策略"));

        mockMvc.perform(multipart("/directories/{directoryId}/files", DIRECTORY_ID)
                        .file(file)
                        .param("projectId", PROJECT_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("存在同名文件，请选择处理策略"))
                .andExpect(jsonPath("$.data.options[0]").value("overwrite"))
                .andExpect(jsonPath("$.data.options[1]").value("rename"))
                .andExpect(jsonPath("$.data.options[2]").value("new_version"));
    }

    @Test
    @DisplayName("POST /directories/{directoryId}/files 超过大小限制时拒绝上传")
    void shouldRejectUploadWhenFileExceedsSizeLimit() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large.bin",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                new byte[1025]
        );

        mockMvc.perform(multipart("/directories/{directoryId}/files", DIRECTORY_ID)
                        .file(file)
                        .param("projectId", PROJECT_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("上传文件不能超过 1024 字节"));

        then(uploadFileUseCase).should(never()).upload(any());
    }

    @Test
    @DisplayName("GET /files/{fileId}/download 返回下载内容和文件名")
    void shouldDownloadFile() throws Exception {
        DownloadResponse response = new DownloadResponse("README.md", MediaType.TEXT_MARKDOWN_VALUE, "# 说明".getBytes());
        given(downloadFileUseCase.download(FILE_ID)).willReturn(response);

        mockMvc.perform(get("/files/{fileId}/download", FILE_ID))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"README.md\""))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_MARKDOWN))
                .andExpect(content().bytes("# 说明".getBytes()));
    }

    @Test
    @DisplayName("POST /files/{fileId}/move 移动文件到目标目录")
    void shouldMoveFile() throws Exception {
        MoveFileRequest request = new MoveFileRequest(PROJECT_ID, "directory-target");
        FileItemResponse response = new FileItemResponse(FILE_ID, "README.md", 128L, "text/markdown", 1, "active", LocalDateTime.parse("2026-05-24T10:00:00"));
        given(moveFileUseCase.move(eq(FILE_ID), any())).willReturn(response);

        mockMvc.perform(post("/files/{fileId}/move", FILE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectId": "project-001",
                                  "targetDirectoryId": "directory-target"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fileId").value(FILE_ID))
                .andExpect(jsonPath("$.data.status").value("active"));

        then(moveFileUseCase).should().move(FILE_ID, request);
    }

    @Test
    @DisplayName("DELETE /files/{fileId} 删除文件进入回收站")
    void shouldMoveFileToTrash() throws Exception {
        given(deleteFileUseCase.delete(any())).willReturn(new DeleteFileResult(trashedFile()));

        mockMvc.perform(delete("/files/{fileId}", FILE_ID)
                        .param("projectId", PROJECT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fileId").value(FILE_ID))
                .andExpect(jsonPath("$.data.status").value("trashed"));
    }

    @Test
    @DisplayName("GET /projects/{projectId}/trash 返回回收站文件")
    void shouldListTrashFiles() throws Exception {
        TrashFileResponse trashFile = new TrashFileResponse(FILE_ID, "README.md", DIRECTORY_ID, "user-001", Instant.parse("2026-05-24T10:15:30Z"));
        given(listTrashFilesUseCase.listTrash(PROJECT_ID)).willReturn(List.of(trashFile));

        mockMvc.perform(get("/projects/{projectId}/trash", PROJECT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].fileId").value(FILE_ID))
                .andExpect(jsonPath("$.data[0].originalDirectoryId").value(DIRECTORY_ID));
    }

    @Test
    @DisplayName("POST /files/{fileId}/restore 恢复文件时表达 restoreDirectoryId")
    void shouldRestoreFileWithRestoreDirectoryId() throws Exception {
        given(restoreFileUseCase.restore(any())).willReturn(RestoreFileResult.restored(activeFile(1)));

        mockMvc.perform(post("/files/{fileId}/restore", FILE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectId": "project-001",
                                  "restoreDirectoryId": "directory-target"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fileId").value(FILE_ID))
                .andExpect(jsonPath("$.data.status").value("active"));

        then(restoreFileUseCase).should().restore(any());
    }

    @Test
    @DisplayName("POST /files/{fileId}/restore 缺少可恢复目录时返回统一错误结构")
    void shouldReturnValidationErrorWhenRestoreDirectoryIsMissing() throws Exception {
        given(restoreFileUseCase.restore(any())).willThrow(new IllegalArgumentException("restoreDirectoryId 不能为空"));

        mockMvc.perform(post("/files/{fileId}/restore", FILE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectId": "project-001"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("restoreDirectoryId 不能为空"));
    }

    /**
     * 创建测试文件实体，用于模拟应用层返回可见文件。
     *
     * @param versionNo 文件版本号
     * @return 正常状态的测试文件实体
     */
    private FileAsset activeFile(int versionNo) {
        return FileAsset.uploaded(FILE_ID, PROJECT_ID, DIRECTORY_ID, FileName.of("README.md"), 128L, "text/markdown", "storage/readme", "user-001", LocalDateTime.now(), "version-group-001", versionNo);
    }

    /**
     * 创建测试回收站文件实体，用于模拟删除接口返回值。
     *
     * @return 已进入回收站的测试文件实体
     */
    private FileAsset trashedFile() {
        FileAsset file = activeFile(1);
        file.moveToTrash("user-001", LocalDateTime.parse("2026-05-24T18:15:30"));
        return file;
    }
}
