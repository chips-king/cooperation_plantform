package com.cooperation.application.group;

import com.cooperation.web.group.GroupDto;

/**
 * 查询小组详情应用用例契约，供 Web 层读取单个小组基础信息。
 */
public interface GetGroupDetailUseCase {

    /**
     * 查询当前用户可见的小组详情。
     *
     * @param query 小组详情查询参数。
     * @return 小组详情响应数据。
     */
    GroupDto.GroupResponse get(Query query);

    /**
     * 小组详情查询参数。
     *
     * @param actorId 当前操作用户标识。
     * @param groupId 小组标识。
     */
    record Query(Long actorId, Long groupId) {
    }
}
