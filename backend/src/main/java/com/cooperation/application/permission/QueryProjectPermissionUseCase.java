package com.cooperation.application.permission;

import com.cooperation.web.permission.PermissionDto.ProjectPermissionResponse;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * 项目权限查询应用用例，提供成员权限列表查询的最小端口式契约。
 */
@Service
public class QueryProjectPermissionUseCase {

    /**
     * 查询项目权限列表。
     *
     * @param query 权限查询条件。
     * @return 项目权限响应。
     */
    public ProjectPermissionResponse query(Query query) {
        Objects.requireNonNull(query, "权限查询条件不能为空");
        return new ProjectPermissionResponse(query.projectId(), List.of());
    }

    /**
     * 项目权限查询条件。
     *
     * @param operatorId 当前用户标识。
     * @param projectId 项目标识。
     */
    public record Query(Long operatorId, Long projectId) {
    }
}
