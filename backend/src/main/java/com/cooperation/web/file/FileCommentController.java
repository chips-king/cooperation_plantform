package com.cooperation.web.file;

import com.cooperation.application.file.FileCommentUseCase;
import com.cooperation.web.common.ApiResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * 文件评论 Web API 控制器，提供评论的查询、添加和删除接口。
 */
@RestController
public class FileCommentController {

    private final FileCommentUseCase fileCommentUseCase;

    /**
     * 创建文件评论控制器。
     *
     * @param fileCommentUseCase 文件评论用例。
     */
    public FileCommentController(FileCommentUseCase fileCommentUseCase) {
        this.fileCommentUseCase = fileCommentUseCase;
    }

    /**
     * 获取指定文件的评论列表。
     *
     * @param fileId 文件标识。
     * @return 评论列表响应。
     */
    @GetMapping("/files/{fileId}/comments")
    public ApiResponse<FileCommentUseCase.ListResult> listComments(@PathVariable String fileId) {
        return ApiResponse.success(fileCommentUseCase.listByFile(fileId));
    }

    /**
     * 添加评论到指定文件。
     *
     * @param fileId 文件标识。
     * @param userId 当前用户标识。
     * @param username 当前用户名。
     * @param request 评论请求体。
     * @return 新创建的评论。
     */
    @PostMapping("/files/{fileId}/comments")
    public ApiResponse<CommentResponse> addComment(
            @PathVariable String fileId,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") long userId,
            @RequestHeader(value = "X-Username", defaultValue = "未知用户") String username,
            @RequestBody AddCommentRequest request
    ) {
        FileCommentUseCase.CommentItem comment = fileCommentUseCase.add(
                new FileCommentUseCase.AddCommand(fileId, userId, username, request.content())
        );
        return ApiResponse.success(toResponse(comment));
    }

    /**
     * 删除指定评论，仅允许评论作者操作。
     *
     * @param commentId 评论标识。
     * @param userId 当前用户标识。
     * @return 操作结果。
     */
    @DeleteMapping("/comments/{commentId}")
    public ApiResponse<Void> deleteComment(
            @PathVariable long commentId,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") long userId
    ) {
        fileCommentUseCase.delete(commentId, userId);
        return ApiResponse.successWithoutData();
    }

    private CommentResponse toResponse(FileCommentUseCase.CommentItem item) {
        return new CommentResponse(
                item.id(),
                item.fileId(),
                item.userId(),
                item.username(),
                item.content(),
                item.createdAt()
        );
    }

    /**
     * 添加评论请求体。
     *
     * @param content 评论内容。
     */
    public record AddCommentRequest(String content) {
    }

    /**
     * 评论响应数据。
     *
     * @param id 评论标识。
     * @param fileId 文件标识。
     * @param userId 评论人标识。
     * @param username 评论人用户名。
     * @param content 评论内容。
     * @param createdAt 创建时间。
     */
    public record CommentResponse(
            Long id,
            String fileId,
            long userId,
            String username,
            String content,
            Instant createdAt
    ) {
    }
}
