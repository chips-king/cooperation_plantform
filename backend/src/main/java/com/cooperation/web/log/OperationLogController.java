package com.cooperation.web.log;

import com.cooperation.application.log.ListOperationLogsUseCase;
import com.cooperation.domain.log.OperationAction;
import com.cooperation.web.common.ApiResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Optional;

/**
 * 操作记录 Web API 控制器，负责项目操作记录列表查询和筛选。
 */
@RestController
public class OperationLogController {

    private final ListOperationLogsUseCase listOperationLogsUseCase;

    /**
     * 创建操作记录控制器实例。
     *
     * @param listOperationLogsUseCase 操作记录列表查询用例。
     */
    public OperationLogController(ListOperationLogsUseCase listOperationLogsUseCase) {
        this.listOperationLogsUseCase = listOperationLogsUseCase;
    }

    /**
     * 查询指定项目的操作记录列表。
     *
     * @param headerUserId 请求头中的当前用户标识。
     * @param projectId 项目标识。
     * @param action 操作类型筛选。
     * @param actorId 操作人筛选。
     * @param from 开始时间筛选。
     * @param to 结束时间筛选。
     * @return 统一操作记录列表响应。
     */
    @GetMapping("/projects/{projectId}/operation-logs")
    public ApiResponse<ListOperationLogsUseCase.Result> list(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @PathVariable String projectId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Long actorId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) {
        ListOperationLogsUseCase.Query query = new ListOperationLogsUseCase.Query(
                currentUserId(headerUserId),
                projectId,
                optionalAction(action),
                Optional.ofNullable(actorId),
                optionalInstant(from),
                optionalInstant(to)
        );
        return ApiResponse.success(listOperationLogsUseCase.handle(query));
    }

    private Long currentUserId(Long headerUserId) {
        if (headerUserId != null) {
            return headerUserId;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalArgumentException("当前用户标识不能为空");
        }
        return Long.valueOf(authentication.getName());
    }

    private Optional<OperationAction> optionalAction(String action) {
        return Optional.ofNullable(action)
                .filter(value -> !value.isBlank())
                .map(OperationAction::valueOf);
    }

    private Optional<Instant> optionalInstant(String value) {
        return Optional.ofNullable(value)
                .filter(item -> !item.isBlank())
                .map(Instant::parse);
    }
}
