package com.cooperation.web.project;

import com.cooperation.application.project.CreateProjectUseCase;
import com.cooperation.application.project.DeleteProjectUseCase;
import com.cooperation.application.project.EndProjectUseCase;
import com.cooperation.application.project.GetProjectDetailUseCase;
import com.cooperation.application.project.ReopenProjectUseCase;
import com.cooperation.domain.project.Project;
import com.cooperation.domain.project.ProjectRepository;
import com.cooperation.web.common.ApiResponse;
import com.cooperation.web.common.PageResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 项目 Web API 控制器，负责项目创建、详情、结束和重新打开。
 */
@RestController
public class ProjectController {

    private final CreateProjectUseCase createProjectUseCase;
    private final GetProjectDetailUseCase getProjectDetailUseCase;
    private final EndProjectUseCase endProjectUseCase;
    private final ReopenProjectUseCase reopenProjectUseCase;
    private final DeleteProjectUseCase deleteProjectUseCase;
    private final ProjectRepository projectRepository;

    /**
     * 创建项目控制器实例。
     *
     * @param createProjectUseCase 创建项目用例。
     * @param getProjectDetailUseCase 查询项目详情用例。
     * @param endProjectUseCase 结束项目用例。
     * @param reopenProjectUseCase 重新打开项目用例。
     * @param projectRepository 项目仓储。
     */
    public ProjectController(
            CreateProjectUseCase createProjectUseCase,
            GetProjectDetailUseCase getProjectDetailUseCase,
            EndProjectUseCase endProjectUseCase,
            ReopenProjectUseCase reopenProjectUseCase,
            DeleteProjectUseCase deleteProjectUseCase,
            ProjectRepository projectRepository
    ) {
        this.createProjectUseCase = createProjectUseCase;
        this.getProjectDetailUseCase = getProjectDetailUseCase;
        this.endProjectUseCase = endProjectUseCase;
        this.reopenProjectUseCase = reopenProjectUseCase;
        this.deleteProjectUseCase = deleteProjectUseCase;
        this.projectRepository = projectRepository;
    }

    /**
     * 在指定小组下创建项目。
     *
     * @param actorId 当前操作用户标识。
     * @param groupId 小组标识。
     * @param request 创建项目请求。
     * @return 统一创建项目响应。
     */
    @PostMapping("/groups/{groupId}/projects")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProjectDto.CreateProjectResponse> create(
            @RequestHeader("X-User-Id") Long actorId,
            @PathVariable Long groupId,
            @Valid @RequestBody ProjectDto.CreateProjectRequest request
    ) {
        CreateProjectUseCase.Result result = createProjectUseCase.create(
                new CreateProjectUseCase.Command(actorId, groupId, request.name())
        );
        return ApiResponse.success(new ProjectDto.CreateProjectResponse(
                result.projectId(),
                result.status().getValue(),
                result.updatedAt()
        ));
    }

    /**
     * 查询项目详情。
     *
     * @param actorId 当前操作用户标识。
     * @param projectId 项目标识。
     * @return 统一项目详情响应。
     */
    @GetMapping("/projects/{projectId}")
    public ApiResponse<ProjectDto.ProjectDetailResponse> get(
            @RequestHeader("X-User-Id") Long actorId,
            @PathVariable Long projectId
    ) {
        return ApiResponse.success(getProjectDetailUseCase.get(new GetProjectDetailUseCase.Query(actorId, projectId)));
    }

    /**
     * 查询当前用户最近参与项目列表。
     *
     * @param actorId 当前操作用户标识。
     * @param page 当前页码。
     * @param size 每页数量。
     * @return 统一分页项目响应。
     */
    @GetMapping("/projects")
    public ApiResponse<PageResponse<ProjectDto.ProjectDetailResponse>> list(
            @RequestHeader("X-User-Id") Long actorId,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "1") int page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "20") int size
    ) {
        List<ProjectDto.ProjectDetailResponse> items = projectRepository.findRecentByUserId(actorId, size).stream()
                .map(this::toProjectDetailResponse)
                .toList();
        return ApiResponse.success(new PageResponse<>(items, page, size, items.size()));
    }

    /**
     * 结束项目。
     *
     * @param actorId 当前操作用户标识。
     * @param projectId 项目标识。
     * @return 统一项目详情响应。
     */
    @PostMapping("/projects/{projectId}/end")
    public ApiResponse<ProjectDto.ProjectDetailResponse> end(
            @RequestHeader("X-User-Id") Long actorId,
            @PathVariable Long projectId
    ) {
        EndProjectUseCase.Result result = endProjectUseCase.handle(new EndProjectUseCase.Command(projectId, actorId));
        return ApiResponse.success(toProjectDetailResponse(result.project()));
    }

    /**
     * 重新打开项目。
     *
     * @param actorId 当前操作用户标识。
     * @param projectId 项目标识。
     * @return 统一项目详情响应。
     */
    @PostMapping("/projects/{projectId}/reopen")
    public ApiResponse<ProjectDto.ProjectDetailResponse> reopen(
            @RequestHeader("X-User-Id") Long actorId,
            @PathVariable Long projectId
    ) {
        ReopenProjectUseCase.Result result = reopenProjectUseCase.handle(
                new ReopenProjectUseCase.Command(projectId, actorId)
        );
        return ApiResponse.success(toProjectDetailResponse(result.project()));
    }

    /**
     * 删除项目。
     */
    @DeleteMapping("/projects/{projectId}")
    @Transactional
    public ApiResponse<ProjectDto.DeleteProjectResponse> delete(
            @RequestHeader("X-User-Id") Long actorId,
            @PathVariable Long projectId
    ) {
        DeleteProjectUseCase.Result result = deleteProjectUseCase.delete(new DeleteProjectUseCase.Command(projectId, actorId));
        return ApiResponse.success(new ProjectDto.DeleteProjectResponse(result.deletedProjectId()));
    }

    private ProjectDto.ProjectDetailResponse toProjectDetailResponse(Project project) {
        return new ProjectDto.ProjectDetailResponse(
                project.getId(),
                project.getGroupId(),
                project.getName(),
                project.getOwnerId(),
                project.getStatus().getValue(),
                project.getEndedAt(),
                project.getReopenedAt()
        );
    }
}
