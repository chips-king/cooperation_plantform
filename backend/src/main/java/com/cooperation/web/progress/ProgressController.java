package com.cooperation.web.progress;

import com.cooperation.application.directory.ListProjectProgressUseCase;
import com.cooperation.application.directory.UpdateDirectoryStatusCommand;
import com.cooperation.application.directory.UpdateDirectoryStatusResult;
import com.cooperation.application.directory.UpdateDirectoryStatusUseCase;
import com.cooperation.domain.directory.DirectoryNode;
import com.cooperation.domain.directory.DirectoryStatus;
import com.cooperation.web.common.ApiResponse;
import com.cooperation.web.progress.ProgressDto.DirectoryStatusResponse;
import com.cooperation.web.progress.ProgressDto.ProjectProgressResponse;
import com.cooperation.web.progress.ProgressDto.UpdateDirectoryStatusRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 项目进度 Web 控制器，提供目录进度查询和目录状态更新接口。
 */
@RestController
public class ProgressController {

    private static final String DEFAULT_OPERATOR_ID = "user-001";

    private final ListProjectProgressUseCase listProjectProgressUseCase;
    private final UpdateDirectoryStatusUseCase updateDirectoryStatusUseCase;

    /**
     * 创建项目进度控制器。
     *
     * @param listProjectProgressUseCase 项目进度查询用例。
     * @param updateDirectoryStatusUseCase 目录状态更新用例。
     */
    public ProgressController(
            ListProjectProgressUseCase listProjectProgressUseCase,
            UpdateDirectoryStatusUseCase updateDirectoryStatusUseCase
    ) {
        this.listProjectProgressUseCase = listProjectProgressUseCase;
        this.updateDirectoryStatusUseCase = updateDirectoryStatusUseCase;
    }

    /**
     * 查询项目目录进度。
     *
     * @param projectId 项目标识。
     * @return 统一项目进度响应。
     */
    @GetMapping("/projects/{projectId}/progress")
    public ApiResponse<ProjectProgressResponse> getProgress(@PathVariable String projectId) {
        return ApiResponse.success(listProjectProgressUseCase.getProgress(projectId));
    }

    /**
     * 更新目录状态。
     *
     * @param directoryId 目录标识。
     * @param request 更新目录状态请求。
     * @return 统一目录状态响应。
     */
    @PatchMapping("/directories/{directoryId}/status")
    public ApiResponse<DirectoryStatusResponse> updateStatus(
            @PathVariable String directoryId,
            @RequestBody UpdateDirectoryStatusRequest request
    ) {
        DirectoryStatus nextStatus = toDirectoryStatus(request.status());
        UpdateDirectoryStatusResult result = updateDirectoryStatusUseCase.update(new UpdateDirectoryStatusCommand(
                request.projectId(),
                directoryId,
                DEFAULT_OPERATOR_ID,
                nextStatus
        ));
        return ApiResponse.success(toStatusResponse(directoryId, result.directory()));
    }

    private DirectoryStatusResponse toStatusResponse(String directoryId, DirectoryNode directory) {
        DirectoryStatus status = directory.getStatus();
        return new DirectoryStatusResponse(
                directoryId,
                directory.getName(),
                status.getValue(),
                status.getDisplayName()
        );
    }

    private DirectoryStatus toDirectoryStatus(String value) {
        return switch (value) {
            case "not_started" -> DirectoryStatus.NOT_STARTED;
            case "in_progress" -> DirectoryStatus.IN_PROGRESS;
            case "completed" -> DirectoryStatus.COMPLETED;
            default -> throw new IllegalArgumentException("目录状态仅允许 not_started、in_progress、completed");
        };
    }
}
