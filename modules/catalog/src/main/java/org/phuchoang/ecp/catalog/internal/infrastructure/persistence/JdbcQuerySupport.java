package org.phuchoang.ecp.catalog.internal.infrastructure.persistence;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;
import java.util.Optional;

/**
 * Template Method base for catalog JDBC query objects. It centralizes the repeated cardinality
 * handling while concrete query objects retain ownership of their SQL and row mappings.
 */
public abstract class JdbcQuerySupport {

    protected final JdbcClient jdbc;

    protected JdbcQuerySupport(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    protected final boolean exists(String sql, Object... parameters) {
        return Boolean.TRUE.equals(jdbc.sql(sql).params(parameters).query(Boolean.class).single());
    }

    protected final <T> Optional<T> optional(String sql, RowMapper<T> rowMapper, Object... parameters) {
        return jdbc.sql(sql).params(parameters).query(rowMapper).optional();
    }

    protected final <T> T required(String sql, Class<T> type, Object... parameters) {
        return jdbc.sql(sql).params(parameters).query(type).single();
    }

    protected final <T> List<T> list(String sql, RowMapper<T> rowMapper, Object... parameters) {
        return jdbc.sql(sql).params(parameters).query(rowMapper).list();
    }
}
