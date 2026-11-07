package vn.medora.api.common;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class AuditService {
    private final JdbcTemplate jdbc;
    public AuditService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    // Store references and action names, never passwords or clinical content.
    @Transactional
    public void record(UUID actor, UUID facility, String action, String type, UUID target) {
        record(actor, facility, action, type, target, null, null);
    }

    @Transactional
    public void record(UUID actor, UUID facility, String action, String type, UUID target,
        UUID related, String detail) {
        jdbc.update("""
            insert into medora.audit_event
                (id, actor_id, facility_id, action, target_type, target_id, related_id, detail)
            values (?, ?, ?, ?, ?, ?, ?, ?)
            """, UUID.randomUUID(), actor, facility, action, type, target, related, detail);
    }
}
