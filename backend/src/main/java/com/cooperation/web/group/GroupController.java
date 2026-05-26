package com.cooperation.web.group;

import com.cooperation.application.group.CreateGroupUseCase;
import com.cooperation.application.group.GetGroupDetailUseCase;
import com.cooperation.application.group.ListGroupsUseCase;
import com.cooperation.web.common.ApiResponse;
import com.cooperation.web.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 小组 Web API 控制器，负责小组创建、列表和详情查询。
 */
@RestController
public class GroupController {

    private final CreateGroupUseCase createGroupUseCase;
    private final ListGroupsUseCase listGroupsUseCase;
    private final GetGroupDetailUseCase getGroupDetailUseCase;

    /**
     * 创建小组控制器实例。
     *
     * @param createGroupUseCase 创建小组用例。
     * @param listGroupsUseCase 查询小组列表用例。
     * @param getGroupDetailUseCase 查询小组详情用例。
     */
    public GroupController(
            CreateGroupUseCase createGroupUseCase,
            ListGroupsUseCase listGroupsUseCase,
            GetGroupDetailUseCase getGroupDetailUseCase
    ) {
        this.createGroupUseCase = createGroupUseCase;
        this.listGroupsUseCase = listGroupsUseCase;
        this.getGroupDetailUseCase = getGroupDetailUseCase;
    }

    /**
     * 创建小组。
     *
     * @param actorId 当前操作用户标识。
     * @param request 创建小组请求。
     * @return 统一创建小组响应。
     */
    @PostMapping("/groups")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<GroupDto.CreateGroupResponse> create(
            @RequestHeader("X-User-Id") Long actorId,
            @Valid @RequestBody GroupDto.CreateGroupRequest request
    ) {
        CreateGroupUseCase.Result result = createGroupUseCase.create(
                new CreateGroupUseCase.Command(actorId, request.name())
        );
        return ApiResponse.success(new GroupDto.CreateGroupResponse(result.groupId()));
    }

    /**
     * 查询当前用户的小组列表。
     *
     * @param actorId 当前操作用户标识。
     * @param page 当前页码。
     * @param size 每页数量。
     * @return 统一分页小组响应。
     */
    @GetMapping("/groups")
    public ApiResponse<PageResponse<GroupDto.GroupResponse>> list(
            @RequestHeader("X-User-Id") Long actorId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        ListGroupsUseCase.Result result = listGroupsUseCase.list(new ListGroupsUseCase.Query(actorId, page, size));
        return ApiResponse.success(new PageResponse<>(result.items(), result.page(), result.size(), result.total()));
    }

    /**
     * 查询小组详情。
     *
     * @param actorId 当前操作用户标识。
     * @param groupId 小组标识。
     * @return 统一小组详情响应。
     */
    @GetMapping("/groups/{groupId}")
    public ApiResponse<GroupDto.GroupResponse> get(
            @RequestHeader("X-User-Id") Long actorId,
            @PathVariable Long groupId
    ) {
        return ApiResponse.success(getGroupDetailUseCase.get(new GetGroupDetailUseCase.Query(actorId, groupId)));
    }
}
