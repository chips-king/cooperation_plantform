package com.cooperation.application.group;

import java.util.Optional;

/**
 * 小组应用仓储接口，用于隔离创建小组用例与持久化实现。
 */
public interface GroupRepository {

    /**
     * 保存小组。
     *
     * @param group 待保存的小组实体。
     * @return 保存后的小组实体。
     */
    Group save(Group group);

    /**
     * 按小组标识查询小组。
     *
     * @param id 小组标识。
     * @return 找到时返回小组，否则返回空。
     */
    Optional<Group> findById(Long id);
}
