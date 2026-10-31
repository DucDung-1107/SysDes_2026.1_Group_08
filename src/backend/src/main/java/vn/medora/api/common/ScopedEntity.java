package vn.medora.api.common;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@MappedSuperclass
public abstract class ScopedEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    protected UUID id;
    @Column(name = "facility_id", nullable = false, updatable = false)
    protected UUID facilityId;
    @Version @Column(nullable = false)
    protected Long version;
    @Column(name = "created_at", nullable = false, updatable = false)
    protected Instant createdAt = Instant.now();

    public UUID getId() { return id; }
    public UUID getFacilityId() { return facilityId; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}
