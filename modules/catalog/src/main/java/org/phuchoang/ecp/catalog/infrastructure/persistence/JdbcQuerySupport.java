package org.phuchoang.ecp.catalog.infrastructure.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;
import java.util.Optional;

/**
 * Template Method base for catalog JDBC query objects. It centralizes the repeated cardinality
 * handling while concrete query objects retain ownership of their SQL and row mappings.
 */
public abstract class JdbcQuerySupport {

    protected final JdbcTemplate jdbc;

    protected JdbcQuerySupport(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    protected final boolean exists(String sql, Object... parameters) {
        return Boolean.TRUE.equals(jdbc.queryForObject(sql, Boolean.class, parameters));
    }

    protected final <T> Optional<T> optional(String sql, RowMapper<T> rowMapper, Object... parameters) {
        return jdbc.query(sql, rowMapper, parameters).stream().findFirst();
    }

    protected final <T> T required(String sql, Class<T> type, Object... parameters) {
        return jdbc.queryForObject(sql, type, parameters);
    }

    protected final <T> List<T> list(String sql, RowMapper<T> rowMapper, Object... parameters) {
        return jdbc.query(sql, rowMapper, parameters);
    }
}
