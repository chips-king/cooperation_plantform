package com.cooperation.domain.log;

import java.util.List;
import java.util.Optional;

/**
 * 操作记录仓储抽象。
 */
public interface OperationLogRepository {

    /**
     * 保存操作记录。
     *
     * @param operationLog 待保存的操作记录
     * @return 保存后的操作记录
     */
    OperationLog save(OperationLog operationLog);

    /**
     * 按项目查询操作记录。
     *
     * @param projectId 项目标识
     * @return 项目下的操作记录列表
     */
    List<OperationLog> findByProjectId(String projectId);

    /**
     * 按项目和动作类型查询操作记录。
     *
     * @param projectId 项目标识
     * @param action 操作动作类型
     * @return 匹配条件的操作记录列表
     */
    List<OperationLog> findByProjectIdAndAction(String projectId, OperationAction action);

    /**
     * 按项目和操作人查询操作记录。
     *
     * @param projectId 项目标识
     * @param actorId 操作人标识
     * @return 匹配条件的操作记录列表
     */
    List<OperationLog> findByProjectIdAndActorId(String projectId, String actorId);

    /**
     * 按记录标识查询操作记录。
     *
     * @param id 操作记录标识
     * @return 匹配的操作记录
     */
    Optional<OperationLog> findById(String id);
}
