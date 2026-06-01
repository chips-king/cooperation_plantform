package com.cooperation.infrastructure.persistence;

import com.cooperation.application.mail.SmtpConfigRepository;
import com.cooperation.domain.mail.SmtpConfig;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

/**
 * SMTP 配置的 JDBC 持久化实现。
 */
public class JdbcSmtpConfigRepository implements SmtpConfigRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcSmtpConfigRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public SmtpConfig save(SmtpConfig config) {
        if (config.getId() != null && existsById(config.getId())) {
            jdbcTemplate.update("""
                    UPDATE smtp_configs SET name = ?, host = ?, port = ?, username = ?, password = ?,
                    from_address = ?, imap_host = ?, imap_port = ?,
                    ssl_enabled = ?, starttls_enabled = ?, is_default = ?
                    WHERE id = ?
                    """, config.getName(), config.getHost(), config.getPort(),
                    config.getUsername(), config.getPassword(), config.getFromAddress(),
                    config.getImapHost(), config.getImapPort(),
                    config.isSslEnabled(), config.isStarttlsEnabled(), config.isDefault(),
                    config.getId());
            return config;
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO smtp_configs (name, host, port, username, password, from_address,
                    imap_host, imap_port, ssl_enabled, starttls_enabled, is_default, created_by)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, config.getName());
            ps.setString(2, config.getHost());
            ps.setInt(3, config.getPort());
            ps.setString(4, config.getUsername());
            ps.setString(5, config.getPassword());
            ps.setString(6, config.getFromAddress());
            ps.setString(7, config.getImapHost());
            ps.setInt(8, config.getImapPort());
            ps.setBoolean(9, config.isSslEnabled());
            ps.setBoolean(10, config.isStarttlsEnabled());
            ps.setBoolean(11, config.isDefault());
            ps.setLong(12, config.getCreatedBy());
            return ps;
        }, keyHolder);

        Long id = keyHolder.getKey() == null ? null : keyHolder.getKey().longValue();
        return config.withId(id);
    }

    @Override
    public Optional<SmtpConfig> findById(Long id) {
        List<SmtpConfig> results = jdbcTemplate.query(
                "SELECT * FROM smtp_configs WHERE id = ?",
                this::mapRow,
                id
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public List<SmtpConfig> findByCreatedBy(Long createdBy) {
        return jdbcTemplate.query(
                "SELECT * FROM smtp_configs WHERE created_by = ? ORDER BY is_default DESC, created_at ASC",
                this::mapRow,
                createdBy
        );
    }

    @Override
    public Optional<SmtpConfig> findDefaultByCreatedBy(Long createdBy) {
        List<SmtpConfig> results = jdbcTemplate.query(
                "SELECT * FROM smtp_configs WHERE created_by = ? AND is_default = TRUE LIMIT 1",
                this::mapRow,
                createdBy
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public void clearDefault(Long createdBy) {
        jdbcTemplate.update(
                "UPDATE smtp_configs SET is_default = FALSE WHERE created_by = ? AND is_default = TRUE",
                createdBy
        );
    }

    @Override
    public void deleteById(Long id) {
        jdbcTemplate.update("UPDATE mail_drafts SET smtp_config_id = NULL WHERE smtp_config_id = ?", id);
        jdbcTemplate.update("DELETE FROM smtp_configs WHERE id = ?", id);
    }

    private boolean existsById(Long id) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM smtp_configs WHERE id = ?", Integer.class, id
        );
        return count != null && count > 0;
    }

    private SmtpConfig mapRow(java.sql.ResultSet rs, int rowNumber) throws java.sql.SQLException {
        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        String imapHost = rs.getString("imap_host");
        int imapPort = rs.getInt("imap_port");
        return new SmtpConfig(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("host"),
                rs.getInt("port"),
                rs.getString("username"),
                rs.getString("password"),
                rs.getString("from_address"),
                imapHost != null ? imapHost : "",
                imapPort,
                rs.getBoolean("ssl_enabled"),
                rs.getBoolean("starttls_enabled"),
                rs.getBoolean("is_default"),
                rs.getLong("created_by"),
                createdAt != null ? createdAt.toInstant() : null,
                updatedAt != null ? updatedAt.toInstant() : null
        );
    }
}
