package com.cooperation.web.group;

import jakarta.validation.constraints.NotBlank;

/**
 * 小组 Web API 数据传输对象集合。
 */
public final class GroupDto {

    private GroupDto() {
    }

    /**
     * 创建小组请求。
     *
     * @param name 小组名称。
     */
    public record CreateGroupRequest(@NotBlank(message = "小组名称不能为空") String name) {
    }

    /**
     * 创建小组响应。
     *
     * @param groupId 小组标识。
     */
    public record CreateGroupResponse(Long groupId) {
    }

    /**
     * 小组基础响应。
     *
     * @param id 小组标识。
     * @param name 小组名称。
     * @param ownerId 小组负责人用户标识。
     * @param status 小组状态。
     */
    public record GroupResponse(Long id, String name, Long ownerId, String status) {
    }
}
