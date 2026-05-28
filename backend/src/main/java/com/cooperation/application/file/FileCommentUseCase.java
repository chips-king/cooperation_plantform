package com.cooperation.application.file;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * 文件评论用例，提供评论的查询、添加和删除操作。
 */
public class FileCommentUseCase {

    private final CommentPort commentPort;

    /**
     * 创建文件评论用例。
     *
     * @param commentPort 评论持久化端口。
     */
    public FileCommentUseCase(CommentPort commentPort) {
        this.commentPort = Objects.requireNonNull(commentPort, "评论持久化端口不能为空");
    }

    /**
     * 查询指定文件的评论列表，按时间升序返回。
     *
     * @param fileId 文件标识。
     * @return 评论列表结果。
     */
    public ListResult listByFile(String fileId) {
        Objects.requireNonNull(fileId, "文件标识不能为空");
        if (fileId.isBlank()) {
            throw new IllegalArgumentException("文件标识不能为空");
        }
        return new ListResult(commentPort.findByFileId(fileId));
    }

    /**
     * 添加评论到指定文件。
     *
     * @param command 添加评论命令。
     * @return 新创建的评论。
     */
    public CommentItem add(AddCommand command) {
        Objects.requireNonNull(command, "添加评论命令不能为空");
        String content = command.content() == null ? "" : command.content().trim();
        if (content.isEmpty()) {
            throw new IllegalArgumentException("评论内容不能为空");
        }
        if (content.length() > 2000) {
            throw new IllegalArgumentException("评论内容不能超过2000个字符");
        }
        return commentPort.save(new CommentItem(
                null,
                command.fileId(),
                command.userId(),
                command.username(),
                content,
                null
        ));
    }

    /**
     * 删除评论，仅允许评论作者删除自己的评论。
     *
     * @param commentId 评论标识。
     * @param userId 操作人标识。
     */
    public void delete(long commentId, long userId) {
        CommentItem comment = commentPort.findById(commentId);
        if (comment == null) {
            throw new IllegalArgumentException("评论不存在");
        }
        if (comment.userId() != userId) {
            throw new IllegalStateException("只能删除自己的评论");
        }
        commentPort.deleteById(commentId);
    }

    /**
     * 评论持久化端口。
     */
    public interface CommentPort {

        /**
         * 按文件标识查询评论列表。
         *
         * @param fileId 文件标识。
         * @return 评论列表。
         */
        List<CommentItem> findByFileId(String fileId);

        /**
         * 保存评论。
         *
         * @param comment 待保存的评论。
         * @return 保存后的评论（含生成的标识和时间）。
         */
        CommentItem save(CommentItem comment);

        /**
         * 按标识查询评论。
         *
         * @param commentId 评论标识。
         * @return 评论，不存在时返回空。
         */
        CommentItem findById(long commentId);

        /**
         * 按标识删除评论。
         *
         * @param commentId 评论标识。
         */
        void deleteById(long commentId);
    }

    /**
     * 添加评论命令。
     *
     * @param fileId 文件标识。
     * @param userId 评论人标识。
     * @param username 评论人用户名。
     * @param content 评论内容。
     */
    public record AddCommand(String fileId, long userId, String username, String content) {

        /**
         * 校验添加评论命令。
         */
        public AddCommand {
            Objects.requireNonNull(fileId, "文件标识不能为空");
            Objects.requireNonNull(username, "评论人用户名不能为空");
            Objects.requireNonNull(content, "评论内容不能为空");
        }
    }

    /**
     * 评论列表查询结果。
     *
     * @param comments 评论列表。
     */
    public record ListResult(List<CommentItem> comments) {

        /**
         * 规范化评论列表结果。
         */
        public ListResult {
            comments = List.copyOf(Objects.requireNonNull(comments, "评论列表不能为空"));
        }
    }

    /**
     * 评论列表项。
     *
     * @param id 评论标识。
     * @param fileId 文件标识。
     * @param userId 评论人标识。
     * @param username 评论人用户名。
     * @param content 评论内容。
     * @param createdAt 创建时间。
     */
    public record CommentItem(
            Long id,
            String fileId,
            long userId,
            String username,
            String content,
            Instant createdAt
    ) {
    }
}
