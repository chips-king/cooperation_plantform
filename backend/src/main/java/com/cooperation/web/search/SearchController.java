package com.cooperation.web.search;

import com.cooperation.application.search.SearchUseCase;
import com.cooperation.web.common.ApiResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 搜索 Web API 控制器，负责项目、文件和成员搜索。
 */
@RestController
public class SearchController {

    private final SearchUseCase searchUseCase;

    /**
     * 创建搜索控制器实例。
     *
     * @param searchUseCase 全局搜索用例。
     */
    public SearchController(SearchUseCase searchUseCase) {
        this.searchUseCase = searchUseCase;
    }

    /**
     * 按关键字搜索当前用户可见项目。
     *
     * @param headerUserId 请求头中的当前用户标识。
     * @param keyword 搜索关键字。
     * @return 统一搜索结果响应。
     */
    @GetMapping("/search/projects")
    public ApiResponse<SearchUseCase.Result> projects(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.success(searchUseCase.execute(query(headerUserId, keyword)));
    }

    /**
     * 按关键字搜索当前用户可访问文件。
     *
     * @param headerUserId 请求头中的当前用户标识。
     * @param keyword 搜索关键字。
     * @return 统一搜索结果响应。
     */
    @GetMapping("/search/files")
    public ApiResponse<SearchUseCase.Result> files(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.success(searchUseCase.execute(query(headerUserId, keyword)));
    }

    /**
     * 按关键字搜索共同项目范围内的成员。
     *
     * @param headerUserId 请求头中的当前用户标识。
     * @param keyword 搜索关键字。
     * @return 统一搜索结果响应。
     */
    @GetMapping("/search/members")
    public ApiResponse<SearchUseCase.Result> members(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.success(searchUseCase.execute(query(headerUserId, keyword)));
    }

    private SearchUseCase.Query query(Long headerUserId, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException("搜索关键字不能为空");
        }
        return new SearchUseCase.Query(currentUserId(headerUserId), keyword);
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
}
