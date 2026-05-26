package com.cooperation.web.common;

import java.util.List;

/**
 * Web 层分页响应结构。
 *
 * @param items 当前页数据。
 * @param page 当前页码。
 * @param size 每页数量。
 * @param total 总数量。
 * @param <T> 列表元素类型。
 */
public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long total
) {
}
