package com.cooperation.application.mail;

import com.cooperation.domain.mail.SmtpConfig;
import java.util.List;
import java.util.Optional;

/**
 * SMTP 配置仓储端口，管理 SMTP 配置的持久化操作。
 */
public interface SmtpConfigRepository {

    /**
     * 保存 SMTP 配置。
     *
     * @param config SMTP 配置实体
     * @return 保存后的 SMTP 配置
     */
    SmtpConfig save(SmtpConfig config);

    /**
     * 按标识查询 SMTP 配置。
     *
     * @param id 配置标识
     * @return 找到时返回配置，否则返回空
     */
    Optional<SmtpConfig> findById(Long id);

    /**
     * 按创建人查询所有 SMTP 配置。
     *
     * @param createdBy 创建人用户标识
     * @return 该用户的 SMTP 配置列表
     */
    List<SmtpConfig> findByCreatedBy(Long createdBy);

    /**
     * 查询创建人的默认 SMTP 配置。
     *
     * @param createdBy 创建人用户标识
     * @return 默认配置，不存在时为空
     */
    Optional<SmtpConfig> findDefaultByCreatedBy(Long createdBy);

    /**
     * 取消创建人所有配置的默认标记。
     *
     * @param createdBy 创建人用户标识
     */
    void clearDefault(Long createdBy);

    /**
     * 按标识删除 SMTP 配置。
     *
     * @param id 配置标识
     */
    void deleteById(Long id);
}
