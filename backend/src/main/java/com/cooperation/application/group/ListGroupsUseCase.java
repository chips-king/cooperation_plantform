package com.cooperation.application.group;

import com.cooperation.web.group.GroupDto;
import java.util.List;

/**
 * 查询小组列表应用用例契约，供 Web 层按当前用户分页读取小组摘要。
 */
public interface ListGroupsUseCase {

    /**
     * 查询当前用户可见的小组列表。
     *
     * @param query 小组列表查询参数。
     * @return 小组分页查询结果。
     */
    Result list(Query query);

    /**
     * 小组列表查询参数。
     *
     * @param actorId 当前操作用户标识。
     * @param page 当前页码。
     * @param size 每页数量。
     */
    record Query(Long actorId, int page, int size) {
    }

    /**
     * 小组列表查询结果。
     *
     * @param items 当前页小组数据。
     * @param page 当前页码。
     * @param size 每页数量。
     * @param total 总数量。
     */
    record Result(List<GroupDto.GroupResponse> items, int page, int size, long total) {
    }
}
