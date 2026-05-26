package com.cooperation.common.error;

/**
 * 通用错误码枚举。
 */
public enum ErrorCode {

    /** 请求参数或请求体校验失败。 */
    VALIDATION_ERROR,

    /** 当前用户未登录或登录状态失效。 */
    UNAUTHORIZED,

    /** 当前用户无权执行目标操作。 */
    FORBIDDEN,

    /** 请求的资源不存在或不可见。 */
    NOT_FOUND,

    /** 当前操作与资源状态冲突。 */
    CONFLICT,

    /** 服务端出现未预期异常。 */
    INTERNAL_ERROR
}
