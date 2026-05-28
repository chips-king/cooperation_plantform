package com.cooperation.application.member;

import java.util.Optional;

/**
 * 成员关系仓储接口，用于按小组、项目或成员关系标识查询协作身份。
 */
public interface MembershipRepository {

    /**
     * 保存成员关系。
     *
     * @param membership 待保存的成员关系。
     * @return 保存后的成员关系。
     */
    Membership save(Membership membership);

    /**
     * 按成员关系标识查询成员关系。
     *
     * @param id 成员关系标识。
     * @return 找到时返回成员关系，否则返回空。
     */
    default Optional<Membership> findById(Long id) {
        return Optional.empty();
    }

    /**
     * 按小组和用户查询成员关系。
     *
     * @param groupId 小组标识。
     * @param userId 用户标识。
     * @return 找到时返回成员关系，否则返回空。
     */
    Optional<Membership> findByGroupIdAndUserId(Long groupId, Long userId);

    /**
     * 按项目和用户查询成员关系。
     *
     * @param projectId 项目标识。
     * @param userId 用户标识。
     * @return 找到时返回成员关系，否则返回空。
     */
    Optional<Membership> findByProjectIdAndUserId(Long projectId, Long userId);

    /**
     * 按小组标识删除所有成员关系。
     *
     * @param groupId 小组标识。
     */
    void deleteByGroupId(Long groupId);

    /**
     * 按项目标识删除所有成员关系。
     *
     * @param projectId 项目标识。
     */
    void deleteByProjectId(Long projectId);
}
