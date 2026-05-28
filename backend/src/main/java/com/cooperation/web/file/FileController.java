package com.cooperation.web.file;

import com.cooperation.application.file.DeleteFileCommand;
import com.cooperation.application.file.DeleteFileUseCase;
import com.cooperation.application.file.DirectoryManagementUseCase;
import com.cooperation.application.file.DownloadFileUseCase;
import com.cooperation.application.file.EmptyTrashCommand;
import com.cooperation.application.file.EmptyTrashUseCase;
import com.cooperation.application.file.ListDirectoryTreeUseCase;
import com.cooperation.application.file.ListTrashFilesUseCase;
import com.cooperation.application.file.MoveFileUseCase;
import com.cooperation.application.file.RestoreFileCommand;
import com.cooperation.application.file.RestoreFileResult;
import com.cooperation.application.file.RestoreFileUseCase;
import com.cooperation.application.file.UploadDirectoryResolver;
import com.cooperation.application.file.UploadFileCommand;
import com.cooperation.application.file.UploadFileResult;
import com.cooperation.application.file.UploadFileUseCase;
import com.cooperation.domain.file.DuplicateFilePolicy;
import com.cooperation.domain.file.FileAsset;
import com.cooperation.web.common.ApiResponse;
import com.cooperation.web.file.FileDto.DirectoryTreeResponse;
import com.cooperation.web.file.FileDto.DownloadResponse;
import com.cooperation.web.file.FileDto.FileItemResponse;
import com.cooperation.web.file.FileDto.CreateDirectoryRequest;
import com.cooperation.web.file.FileDto.DeleteDirectoryResponse;
import com.cooperation.web.file.FileDto.MoveFileRequest;
import com.cooperation.web.file.FileDto.RestoreFileRequest;
import com.cooperation.web.file.FileDto.TrashFileResponse;
import com.cooperation.web.file.FileDto.UploadFileResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * 文件与目录 Web 控制器，提供目录树、上传、下载、移动、删除、回收站和恢复接口。
 */
@RestController
public class FileController {

    private final ListDirectoryTreeUseCase listDirectoryTreeUseCase;
    private final UploadFileUseCase uploadFileUseCase;
    private final DownloadFileUseCase downloadFileUseCase;
    private final MoveFileUseCase moveFileUseCase;
    private final DeleteFileUseCase deleteFileUseCase;
    private final ListTrashFilesUseCase listTrashFilesUseCase;
    private final RestoreFileUseCase restoreFileUseCase;
    private final EmptyTrashUseCase emptyTrashUseCase;
    private final UploadDirectoryResolver uploadDirectoryResolver;
    private final DirectoryManagementUseCase directoryManagementUseCase;

    /**
     * 创建文件控制器。
     *
     * @param listDirectoryTreeUseCase 目录树查询用例。
     * @param uploadFileUseCase 文件上传用例。
     * @param downloadFileUseCase 文件下载用例。
     * @param moveFileUseCase 文件移动用例。
     * @param deleteFileUseCase 文件删除用例。
     * @param listTrashFilesUseCase 回收站查询用例。
     * @param restoreFileUseCase 文件恢复用例。
     * @param emptyTrashUseCase 清空回收站用例。
     * @param uploadDirectoryResolver 上传目录解析端口。
     * @param directoryManagementUseCase 目录管理用例。
     */
    public FileController(
            ListDirectoryTreeUseCase listDirectoryTreeUseCase,
            UploadFileUseCase uploadFileUseCase,
            DownloadFileUseCase downloadFileUseCase,
            MoveFileUseCase moveFileUseCase,
            DeleteFileUseCase deleteFileUseCase,
            ListTrashFilesUseCase listTrashFilesUseCase,
            RestoreFileUseCase restoreFileUseCase,
            EmptyTrashUseCase emptyTrashUseCase,
            UploadDirectoryResolver uploadDirectoryResolver,
            DirectoryManagementUseCase directoryManagementUseCase
    ) {
        this.listDirectoryTreeUseCase = listDirectoryTreeUseCase;
        this.uploadFileUseCase = uploadFileUseCase;
        this.downloadFileUseCase = downloadFileUseCase;
        this.moveFileUseCase = moveFileUseCase;
        this.deleteFileUseCase = deleteFileUseCase;
        this.listTrashFilesUseCase = listTrashFilesUseCase;
        this.restoreFileUseCase = restoreFileUseCase;
        this.emptyTrashUseCase = emptyTrashUseCase;
        this.uploadDirectoryResolver = uploadDirectoryResolver;
        this.directoryManagementUseCase = directoryManagementUseCase;
    }

    /**
     * 查询项目目录树。
     *
     * @param projectId 项目标识。
     * @return 统一目录树响应。
     */
    @GetMapping("/projects/{projectId}/tree")
    public ApiResponse<DirectoryTreeResponse> getTree(@PathVariable String projectId) {
        return ApiResponse.success(listDirectoryTreeUseCase.getTree(projectId));
    }

    /**
     * 创建分工目录。
     *
     * @param actorId 操作人标识。
     * @param request 创建目录请求。
     * @return 统一目录节点响应。
     */
    @PostMapping("/directories")
    public ApiResponse<DirectoryTreeResponse.DirectoryNodeResponse> createDirectory(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") String actorId,
            @RequestBody CreateDirectoryRequest request
    ) {
        return ApiResponse.success(directoryManagementUseCase.create(new DirectoryManagementUseCase.CreateCommand(
                request.projectId(),
                request.parentDirectoryId(),
                request.name(),
                actorId
        )));
    }

    /**
     * 删除空分工目录。
     *
     * @param directoryId 目录标识。
     * @param actorId 操作人标识。
     * @param projectId 项目标识。
     * @return 被删除目录的父目录定位信息。
     */
    @DeleteMapping("/directories/{directoryId}")
    public ApiResponse<DeleteDirectoryResponse> deleteDirectory(
            @PathVariable String directoryId,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") String actorId,
            @RequestParam String projectId
    ) {
        DirectoryManagementUseCase.DeleteResult result = directoryManagementUseCase.deleteEmpty(new DirectoryManagementUseCase.DeleteCommand(
                projectId,
                directoryId,
                actorId
        ));
        return ApiResponse.success(new DeleteDirectoryResponse(result.parentDirectoryId()));
    }

    /**
     * 上传文件到指定目录。
     *
     * @param directoryId 目标目录标识。
     * @param projectId 项目标识。
     * @param duplicatePolicy 同名文件处理策略。
     * @param relativePath 文件夹上传时浏览器提供的相对路径。
     * @param file 上传的 multipart 文件。
     * @return 统一上传结果响应。
     * @throws IOException 读取上传内容失败时抛出。
     */
    @PostMapping(value = "/directories/{directoryId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UploadFileResponse> upload(
            @PathVariable String directoryId,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") String actorId,
            @RequestParam String projectId,
            @RequestParam(required = false) String duplicatePolicy,
            @RequestParam(required = false) String relativePath,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        String targetDirectoryId = uploadDirectoryResolver.resolveTargetDirectory(projectId, directoryId, relativePath, actorId);
        String filename = relativePath == null || relativePath.isBlank() ? file.getOriginalFilename() : relativePath;
        UploadFileResult result = uploadFileUseCase.upload(new UploadFileCommand(
                projectId,
                targetDirectoryId,
                actorId,
                filename,
                file.getSize(),
                file.getContentType(),
                isArchive(filename),
                toDuplicatePolicy(duplicatePolicy),
                null,
                file.getBytes()
        ));
        return ApiResponse.success(toUploadResponse(result, duplicatePolicy));
    }

    /**
     * 下载指定文件。
     *
     * @param fileId 文件标识。
     * @return 附件下载响应。
     */
    @GetMapping("/files/{fileId}/download")
    public ResponseEntity<byte[]> download(@PathVariable String fileId) {
        DownloadResponse response = downloadFileUseCase.download(fileId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + response.filename() + "\"")
                .contentType(MediaType.parseMediaType(response.mimeType()))
                .body(response.content());
    }

    /**
     * 移动文件到目标目录。
     *
     * @param fileId 文件标识。
     * @param request 移动文件请求。
     * @return 统一文件摘要响应。
     */
    @PostMapping("/files/{fileId}/move")
    public ApiResponse<FileItemResponse> move(@PathVariable String fileId, @RequestBody MoveFileRequest request) {
        return ApiResponse.success(moveFileUseCase.move(fileId, request));
    }

    /**
     * 删除文件并移入回收站。
     *
     * @param fileId 文件标识。
     * @param projectId 项目标识。
     * @return 统一文件摘要响应。
     */
    @DeleteMapping("/files/{fileId}")
    public ApiResponse<FileItemResponse> delete(
            @PathVariable String fileId,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") String actorId,
            @RequestParam String projectId
    ) {
        FileAsset file = deleteFileUseCase.delete(new DeleteFileCommand(projectId, fileId, actorId)).file();
        return ApiResponse.success(FileItemResponse.from(file));
    }

    /**
     * 查询项目回收站文件。
     *
     * @param projectId 项目标识。
     * @return 统一回收站文件列表响应。
     */
    @GetMapping("/projects/{projectId}/trash")
    public ApiResponse<List<TrashFileResponse>> listTrash(@PathVariable String projectId) {
        return ApiResponse.success(listTrashFilesUseCase.listTrash(projectId));
    }

    /**
     * 恢复回收站文件。
     *
     * @param fileId 文件标识。
     * @param request 恢复文件请求。
     * @return 统一文件摘要响应。
     */
    @PostMapping("/files/{fileId}/restore")
    public ApiResponse<FileItemResponse> restore(
            @PathVariable String fileId,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") String actorId,
            @RequestBody RestoreFileRequest request
    ) {
        RestoreFileResult result = restoreFileUseCase.restore(new RestoreFileCommand(
                request.projectId(),
                fileId,
                actorId,
                request.restoreDirectoryId()
        ));
        FileAsset file = result.file().orElseThrow(() -> new IllegalArgumentException("restoreDirectoryId 不能为空"));
        return ApiResponse.success(FileItemResponse.from(file));
    }

    /**
     * 清空项目回收站。
     *
     * @param projectId 项目标识。
     * @param actorId 操作人标识。
     * @return 统一删除数量响应。
     */
    @DeleteMapping("/projects/{projectId}/trash")
    public ApiResponse<java.util.Map<String, Integer>> emptyTrash(
            @PathVariable String projectId,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") String actorId
    ) {
        int count = emptyTrashUseCase.empty(new EmptyTrashCommand(projectId, actorId));
        return ApiResponse.success(java.util.Map.of("deletedCount", count));
    }

    private UploadFileResponse toUploadResponse(UploadFileResult result, String duplicatePolicy) {
        FileAsset file = result.file();
        return new UploadFileResponse(
                file.id(),
                file.name().value(),
                file.size(),
                file.mimeType(),
                duplicatePolicy,
                file.versionNo(),
                file.status().value(),
                result.archive(),
                file.uploadedAt()
        );
    }

    private DuplicateFilePolicy toDuplicatePolicy(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return switch (value) {
            case "overwrite" -> DuplicateFilePolicy.OVERWRITE;
            case "rename" -> DuplicateFilePolicy.RENAME;
            case "new_version" -> DuplicateFilePolicy.NEW_VERSION;
            default -> throw new IllegalArgumentException("同名文件处理策略仅允许 overwrite、rename、new_version");
        };
    }

    private boolean isArchive(String filename) {
        if (filename == null) {
            return false;
        }
        String lowerName = filename.toLowerCase(Locale.ROOT);
        return lowerName.endsWith(".zip") || lowerName.endsWith(".7z") || lowerName.endsWith(".tar.gz");
    }
}
