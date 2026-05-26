package com.cooperation.application.invitation;

import java.util.Optional;

/**
 * 邀请仓储接口，用于保存邀请和按邀请码查询有效邀请。
 */
public interface InvitationRepository {

    /**
     * 保存邀请。
     *
     * @param invitation 待保存的邀请。
     * @return 保存后的邀请。
     */
    Invitation save(Invitation invitation);

    /**
     * 按邀请码查询有效邀请。
     *
     * @param code 邀请码。
     * @return 找到时返回邀请，否则返回空。
     */
    Optional<Invitation> findValidByCode(String code);
}
