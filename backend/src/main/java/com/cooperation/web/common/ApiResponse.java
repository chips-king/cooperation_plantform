package com.cooperation.web.common;

import java.util.List;

/**
 * Web 层统一响应结构。
 *
 * @param success 请求是否成功。
 * @param code 错误码，成功时为空。
 * @param message 响应说明。
 * @param data 响应数据，失败时可为空或携带补充信息。
 * @param fieldErrors 字段校验错误列表。
 * @param <T> 响应数据类型。
 */
public record ApiResponse<T>(
        boolean success,
        String code,
        String message,
        T data,
        List<FieldErrorItem> fieldErrors
) {

    /**
     * 创建成功响应。
     *
     * @param data 响应数据。
     * @param <T> 响应数据类型。
     * @return 统一成功响应。
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, null, "操作成功", data, null);
    }

    /**
     * 创建无数据成功响应。
     *
     * @return 统一成功响应。
     */
    public static ApiResponse<Void> successWithoutData() {
        return success(null);
    }

    /**
     * 创建失败响应。
     *
     * @param code 错误码。
     * @param message 错误说明。
     * @return 统一失败响应。
     */
    public static ApiResponse<Void> failure(String code, String message) {
        return new ApiResponse<>(false, code, message, null, null);
    }

    /**
     * 创建携带补充数据的失败响应。
     *
     * @param code 错误码。
     * @param message 错误说明。
     * @param data 补充数据。
     * @param <T> 补充数据类型。
     * @return 统一失败响应。
     */
    public static <T> ApiResponse<T> failure(String code, String message, T data) {
        return new ApiResponse<>(false, code, message, data, null);
    }

    /**
     * 创建字段校验失败响应。
     *
     * @param message 错误说明。
     * @param fieldErrors 字段错误列表。
     * @return 字段校验失败响应。
     */
    public static ApiResponse<Void> validationFailure(String message, List<FieldErrorItem> fieldErrors) {
        return new ApiResponse<>(false, "VALIDATION_ERROR", message, null, fieldErrors);
    }

    /**
     * 字段错误项。
     *
     * @param field 字段名。
     * @param message 错误说明。
     */
    public record FieldErrorItem(String field, String message) {
    }
}
