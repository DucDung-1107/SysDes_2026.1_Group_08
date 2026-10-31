package vn.medora.api.identity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "identity_audit_event", schema = "medora")
public class IdentityAuditEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "actor_account_id", nullable = false)
    private UUID actorAccountId;
    @Column(name = "target_account_id", nullable = false)
    private UUID targetAccountId;
    @Column(name = "facility_id", nullable = false)
    private UUID facilityId;
    @Column(nullable = false, length = 64)
    private String action;
    @Column(nullable = false, length = 500)
    private String detail;
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected IdentityAuditEvent() { }

    public IdentityAuditEvent(UUID actorAccountId, UUID targetAccountId,
        UUID facilityId, String action, String detail) {
        this.actorAccountId = actorAccountId;
        this.targetAccountId = targetAccountId;
        this.facilityId = facilityId;
        this.action = action;
        this.detail = detail;
        this.occurredAt = Instant.now();
    }
}
