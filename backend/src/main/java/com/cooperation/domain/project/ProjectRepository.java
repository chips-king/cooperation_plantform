package com.cooperation.domain.project;

import java.util.List;
import java.util.Optional;

/**
 * 项目领域仓储抽象，仅描述领域层需要的项目持久化和查询能力。
 */
public interface ProjectRepository {

    /**
     * 保存项目聚合。
     *
     * @param project 待保存的项目聚合。
     * @return 保存后的项目聚合。
     */
    Project save(Project project);

    /**
     * 按项目唯一标识查询项目。
     *
     * @param id 项目唯一标识。
     * @return 找到时返回项目聚合，否则返回空。
     */
    Optional<Project> findById(Long id);

    /**
     * 按用户查询最近参与的项目列表。
     *
     * @param userId 用户唯一标识。
     * @param limit 返回项目数量上限。
     * @return 最近参与项目列表。
     */
    List<Project> findRecentByUserId(Long userId, int limit);

    /**
     * 按小组标识统计项目数量。
     *
     * @param groupId 小组标识。
     * @return 该小组下的项目数量。
     */
    int countByGroupId(Long groupId);

    void deleteById(Long id);
}
