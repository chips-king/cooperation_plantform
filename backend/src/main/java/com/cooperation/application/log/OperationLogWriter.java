package com.cooperation.application.log;

import com.cooperation.domain.log.OperationLog;

/**
 * 应用层操作记录写入端口。
 */
public interface OperationLogWriter {

    /**
     * 写入一条操作记录。
     *
     * @param operationLog 操作记录实体
     */
    void write(OperationLog operationLog);
}
