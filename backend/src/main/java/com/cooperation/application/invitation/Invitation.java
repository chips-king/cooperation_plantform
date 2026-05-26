package com.cooperation.application.invitation;

import java.util.Objects;

/**
 * 邀请应用层实体，表达邀请码、加入模式以及关联的小组和项目。
 */
public final class Invitation {

    private final Long id;
    private final Long groupId;
    private final Long projectId;
    private final String code;
    private final Long inviterId;
    private final JoinMode joinMode;

    private Invitation(Long id, Long groupId, Long projectId, String code, Long inviterId, JoinMode joinMode) {
        this.id = Objects.requireNonNull(id, "邀请标识不能为空");
        this.groupId = Objects.requireNonNull(groupId, "小组标识不能为空");
        this.projectId = Objects.requireNonNull(projectId, "项目标识不能为空");
        this.code = requireText(code, "邀请码不能为空");
        this.inviterId = Objects.requireNonNull(inviterId, "邀请人不能为空");
        this.joinMode = Objects.requireNonNull(joinMode, "加入模式不能为空");
    }

    /**
     * 创建直接加入邀请。
     *
     * @param id 邀请标识。
     * @param groupId 小组标识。
     * @param projectId 项目标识。
     * @param code 邀请码。
     * @param inviterId 邀请人标识。
     * @return 直接加入邀请。
     */
    public static Invitation directJoin(Long id, Long groupId, Long projectId, String code, Long inviterId) {
        return new Invitation(id, groupId, projectId, code, inviterId, JoinMode.DIRECT);
    }

    /**
     * 创建需要审核的邀请。
     *
     * @param id 邀请标识。
     * @param groupId 小组标识。
     * @param projectId 项目标识。
     * @param code 邀请码。
     * @param inviterId 邀请人标识。
     * @return 需要审核的邀请。
     */
    public static Invitation reviewRequired(Long id, Long groupId, Long projectId, String code, Long inviterId) {
        return new Invitation(id, groupId, projectId, code, inviterId, JoinMode.REVIEW_REQUIRED);
    }

    /**
     * 判断邀请是否需要审核。
     *
     * @return 需要审核时返回 true。
     */
    public boolean requiresReview() {
        return joinMode == JoinMode.REVIEW_REQUIRED;
    }

    /**
     * 获取邀请标识。
     *
     * @return 邀请标识。
     */
    public Long getId() {
        return id;
    }

    /**
     * 获取小组标识。
     *
     * @return 小组标识。
     */
    public Long getGroupId() {
        return groupId;
    }

    /**
     * 获取项目标识。
     *
     * @return 项目标识。
     */
    public Long getProjectId() {
        return projectId;
    }

    /**
     * 获取邀请码。
     *
     * @return 邀请码。
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取邀请人标识。
     *
     * @return 邀请人标识。
     */
    public Long getInviterId() {
        return inviterId;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    /**
     * 邀请加入模式。
     */
    public enum JoinMode {
        /** 打开邀请后直接成为成员。 */
        DIRECT,

        /** 打开邀请后生成待审核申请。 */
        REVIEW_REQUIRED
    }
}
