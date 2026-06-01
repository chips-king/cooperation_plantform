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
     * 按登录账号查询用户摘要（匹配展示名称或邮箱）。
     *
     * @param account 登录账号（展示名称或邮箱）。
     * @return 找到时返回用户摘要，否则返回空。
     */
    Optional<UserProfile> findByLoginAccount(String account);

    /**
     * 更新用户展示名称和邮箱。
     *
     * @param id 用户唯一标识。
     * @param displayName 新的展示名称。
     * @param email 新的邮箱。
     * @return 更新成功时返回 true。
     */
    boolean updateProfile(Long id, String displayName, String email);

    /**
     * 查询用户密码哈希值。
     *
     * @param id 用户唯一标识。
     * @return 找到时返回密码哈希值（可能为 null），否则返回空。
     */
    Optional<String> findPasswordHashById(Long id);

    /**
     * 更新用户密码哈希值。
     *
     * @param id 用户唯一标识。
     * @param passwordHash 新的密码哈希值。
     * @return 更新成功时返回 true。
     */
    boolean updatePassword(Long id, String passwordHash);

    /**
     * 按用户名查询用户摘要。
     *
     * @param username 用户名。
     * @return 找到时返回用户摘要，否则返回空。
     */
    Optional<UserProfile> findByUsername(String username);

    /**
     * 创建新用户。
     *
     * @param username 登录用户名。
     * @param displayName 展示名称。
     * @param email 邮箱。
     * @param passwordHash 密码哈希值。
     * @return 创建成功时返回新用户摘要。
     */
    Optional<UserProfile> createUser(String username, String displayName, String email, String passwordHash);

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
