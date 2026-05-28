package com.cooperation.web.common;

import com.cooperation.common.error.ErrorCode;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Web 层全局异常处理器，负责将常见异常映射为统一 JSON 错误结构。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理请求体字段校验失败。
     *
     * @param exception Spring 参数校验异常。
     * @return 统一校验错误响应。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException exception) {
        List<ApiResponse.FieldErrorItem> fieldErrors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toFieldErrorItem)
                .toList();
        return ResponseEntity.badRequest()
                .body(ApiResponse.validationFailure("请求参数校验失败", fieldErrors));
    }

    /**
     * 处理业务参数错误。
     *
     * @param exception 非法参数异常。
     * @return 统一校验错误响应。
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.failure("VALIDATION_ERROR", exception.getMessage()));
    }

    /**
     * 处理业务状态冲突。
     *
     * @param exception 非法状态异常。
     * @return 统一冲突错误响应。
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalState(IllegalStateException exception) {
        if (exception.getMessage() != null && exception.getMessage().contains("同名文件")) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.failure("CONFLICT", exception.getMessage(),
                            java.util.Map.of("options", List.of("overwrite", "rename", "new_version"))));
        }
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure("CONFLICT", exception.getMessage(), null));
    }

    /**
     * 处理权限拒绝。
     *
     * @param exception 权限拒绝异常。
     * @return 统一权限错误响应。
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.failure("FORBIDDEN", exception.getMessage()));
    }

    /**
     * 处理缺少必填请求头。
     *
     * @param exception 缺少请求头异常
     * @return 统一校验错误响应
     */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingRequestHeader(MissingRequestHeaderException exception) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.failure("VALIDATION_ERROR", "缺少请求头: " + exception.getHeaderName()));
    }

    /**
     * 处理数据库唯一约束或外键冲突。
     *
     * @param exception 数据完整性异常
     * @return 统一冲突错误响应
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(DataIntegrityViolationException exception) {
        String detail = exception.getMostSpecificCause().getMessage();
        if (detail != null && detail.contains("uk_package_artifacts_storage_key")) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.failure("CONFLICT", "同名压缩包已存在，请更换文件名后重试"));
        }
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure("CONFLICT", "数据冲突，请刷新后重试"));
    }

    /**
     * 处理未捕获的服务器异常，返回可读错误信息便于前端展示。
     *
     * @param exception 未预期异常
     * @return 统一内部错误响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = "服务器内部错误";
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure(ErrorCode.INTERNAL_ERROR.name(), message));
    }

    private ApiResponse.FieldErrorItem toFieldErrorItem(FieldError error) {
        return new ApiResponse.FieldErrorItem(error.getField(), error.getDefaultMessage());
    }
}
