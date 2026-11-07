package vn.medora.api.common;

import org.springframework.jdbc.core.RowMapper;
import java.sql.*;
import java.time.Instant;
import java.util.*;

public final class SqlRows {
    private SqlRows() { }
    public static final RowMapper<Map<String, Object>> MAPPER = (rs, index) -> {
        Map<String, Object> row = new LinkedHashMap<>();
        var metadata = rs.getMetaData();
        for (int column = 1; column <= metadata.getColumnCount(); column++) {
            String[] parts = metadata.getColumnLabel(column).split("_");
            var name = new StringBuilder(parts[0]);
            for (int i = 1; i < parts.length; i++) {
                name.append(Character.toUpperCase(parts[i].charAt(0))).append(parts[i].substring(1));
            }
            Object value = rs.getObject(column);
            if (value instanceof Timestamp timestamp) { value = timestamp.toInstant(); }
            if (value instanceof java.sql.Date date) { value = date.toLocalDate(); }
            row.put(name.toString(), value);
        }
        return row;
    };
    public static Timestamp time(Instant instant) { return Timestamp.from(instant); }
    public static Instant instant(Map<String, Object> row, String key) { return (Instant) row.get(key); }
    public static UUID uuid(Map<String, Object> row, String key) { return (UUID) row.get(key); }
    public static long number(Map<String, Object> row, String key) { return ((Number) row.get(key)).longValue(); }
}
