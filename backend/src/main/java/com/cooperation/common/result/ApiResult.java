package com.cooperation.common.result;

import com.cooperation.common.error.ErrorCode;

/**
 * 统一 API 响应模型。
 *
 * @param success 请求是否处理成功
 * @param code 业务错误码，成功时为空
 * @param message 面向用户或前端的结果说明
 * @param data 响应数据，失败时通常为空
 * @param <T> 响应数据类型
 */
public record ApiResult<T>(
        boolean success,
        ErrorCode code,
        String message,
        T data
) {

    /**
     * 创建成功响应。
     *
     * @param data 响应数据
     * @param <T> 响应数据类型
     * @return 成功响应对象
     */
    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(true, null, "操作成功", data);
    }

    /**
     * 创建无数据成功响应。
     *
     * @return 无数据成功响应对象
     */
    public static ApiResult<Void> successWithoutData() {
        return new ApiResult<>(true, null, "操作成功", null);
    }

    /**
     * 创建失败响应。
     *
     * @param code 业务错误码
     * @param message 错误说明
     * @return 失败响应对象
     */
    public static ApiResult<Void> failure(ErrorCode code, String message) {
        return new ApiResult<>(false, code, message, null);
    }
}
