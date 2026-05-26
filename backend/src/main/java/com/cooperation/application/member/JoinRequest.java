package com.cooperation.application.member;

import java.util.Objects;

/**
 * 加入申请实体，表达需要审核邀请产生的待处理申请。
 */
public final class JoinRequest {

    private final Long id;
    private final Long userId;
    private final Long invitationId;
    private final Status status;

    private JoinRequest(Long id, Long userId, Long invitationId, Status status) {
        this.id = id;
        this.userId = Objects.requireNonNull(userId, "申请用户不能为空");
        this.invitationId = Objects.requireNonNull(invitationId, "邀请标识不能为空");
        this.status = Objects.requireNonNull(status, "申请状态不能为空");
    }

    /**
     * 创建待审核加入申请。
     *
     * @param userId 申请用户标识。
     * @param invitationId 邀请标识。
     * @return 待审核加入申请。
     */
    public static JoinRequest pending(Long userId, Long invitationId) {
        return new JoinRequest(null, userId, invitationId, Status.PENDING);
    }

    /**
     * 复制当前申请并替换申请标识。
     *
     * @param id 申请标识。
     * @return 带新标识的加入申请。
     */
    public JoinRequest withId(Long id) {
        return new JoinRequest(id, userId, invitationId, status);
    }

    /**
     * 获取申请标识。
     *
     * @return 申请标识，新建未保存时为空。
     */
    public Long getId() {
        return id;
    }

    /**
     * 获取申请用户标识。
     *
     * @return 申请用户标识。
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * 获取邀请标识。
     *
     * @return 邀请标识。
     */
    public Long getInvitationId() {
        return invitationId;
    }

    /**
     * 获取申请状态。
     *
     * @return 申请状态。
     */
    public Status getStatus() {
        return status;
    }

    /**
     * 加入申请状态。
     */
    public enum Status {
        /** 等待负责人审核。 */
        PENDING
    }
}
