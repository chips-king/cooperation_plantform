package com.cooperation.infrastructure.persistence.user;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.cooperation.domain.user.UserRepository;

/**
 * 用户仓储的基础设施实现，负责从 {@code users} 表读取领域层需要的用户摘要。
 */
@Repository
public class MyBatisUserRepository implements UserRepository {

    /**
     * 查询用户摘要所需字段，避免读取领域层暂不需要的持久化字段。
     */
    private static final String SELECT_USER_PROFILE =
            "SELECT id, display_name, email, status FROM users";

    /**
     * Spring JDBC 查询入口，用于执行参数化 SQL 并完成结果集映射。
     */
    private final JdbcTemplate jdbcTemplate;

    /**
     * 创建用户仓储实现。
     *
     * @param jdbcTemplate Spring JDBC 查询入口。
     */
    public MyBatisUserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 按用户唯一标识查询用户摘要。
     *
     * @param id 用户唯一标识。
     * @return 找到时返回用户摘要，否则返回空。
     */
    @Override
    public Optional<UserProfile> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return queryOne(SELECT_USER_PROFILE + " WHERE id = ?", id);
    }

    /**
     * 按邮箱查询用户摘要。
     *
     * @param email 用户邮箱。
     * @return 找到时返回用户摘要，否则返回空。
     */
    @Override
    public Optional<UserProfile> findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return queryOne(SELECT_USER_PROFILE + " WHERE email = ?", email);
    }

    /**
     * 执行单条用户摘要查询，结果集无记录时返回空。
     *
     * @param sql 带单个查询条件的 SQL。
     * @param parameter 查询条件参数。
     * @return 找到时返回用户摘要，否则返回空。
     */
    private Optional<UserProfile> queryOne(String sql, Object parameter) {
        return jdbcTemplate.query(sql, ps -> ps.setObject(1, parameter), rs -> {
            if (!rs.next()) {
                return Optional.empty();
            }
            return Optional.of(toUserProfile(rs));
        });
    }

    /**
     * 将当前结果集行映射为领域层用户摘要。
     *
     * @param resultSet 已定位到用户记录行的结果集。
     * @return 用户摘要。
     * @throws SQLException 读取数据库字段失败时抛出。
     */
    private UserProfile toUserProfile(ResultSet resultSet) throws SQLException {
        return new UserProfile(
                resultSet.getLong("id"),
                resultSet.getString("display_name"),
                resultSet.getString("email"),
                resultSet.getString("status"));
    }
}
