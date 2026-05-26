package com.cooperation.web.common;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
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

    private ApiResponse.FieldErrorItem toFieldErrorItem(FieldError error) {
        return new ApiResponse.FieldErrorItem(error.getField(), error.getDefaultMessage());
    }
}
