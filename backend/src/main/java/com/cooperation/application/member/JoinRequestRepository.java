package com.cooperation.application.member;

/**
 * 加入申请仓储接口，用于保存需要审核的入组申请。
 */
public interface JoinRequestRepository {

    /**
     * 保存加入申请。
     *
     * @param joinRequest 待保存的加入申请。
     * @return 保存后的加入申请。
     */
    JoinRequest save(JoinRequest joinRequest);
}
