package vn.medora.api.common;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.*;
import static vn.medora.api.common.ApiSupport.*;
import static vn.medora.api.common.SqlRows.*;

/** Parameterized SQL gateway shared by the small application modules. */
@Repository
public class Store {
    private final JdbcTemplate jdbc;
    public Store(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    private Object[] params(Object[] values) {
        return Arrays.stream(values).map(v -> v instanceof Instant i ? time(i) : v).toArray();
    }
    public List<Map<String, Object>> list(String sql, Object... values) {
        return jdbc.query(sql, MAPPER, params(values));
    }
    public Optional<Map<String, Object>> optional(String sql, Object... values) {
        return list(sql, values).stream().findFirst();
    }
    public Map<String, Object> one(String sql, Object... values) {
        return optional(sql, values).orElseThrow(ApiSupport::missing);
    }
    public int update(String sql, Object... values) { return jdbc.update(sql, params(values)); }
    public boolean exists(String sql, Object... values) {
        return Boolean.TRUE.equals(jdbc.queryForObject(sql, Boolean.class, params(values)));
    }
    public long count(String sql, Object... values) {
        return Objects.requireNonNull(jdbc.queryForObject(sql, Long.class, params(values)));
    }
}
