package com.cooperation.domain.user;

import java.util.Optional;

/**
 * 用户领域仓储抽象，仅描述领域层需要的用户查询能力。
 */
public interface UserRepository {

    /**
     * 按用户唯一标识查询用户摘要。
     *
     * @param id 用户唯一标识。
     * @return 找到时返回用户摘要，否则返回空。
     */
    Optional<UserProfile> findById(Long id);

    /**
     * 按邮箱查询用户摘要。
     *
     * @param email 用户邮箱。
     * @return 找到时返回用户摘要，否则返回空。
     */
    Optional<UserProfile> findByEmail(String email);

    /**
     * 用户仓储返回的领域摘要，不绑定持久化框架实体。
     *
     * @param id 用户唯一标识。
     * @param displayName 用户展示名称。
     * @param email 用户邮箱。
     * @param status 用户状态。
     */
    record UserProfile(Long id, String displayName, String email, String status) {
    }
}
