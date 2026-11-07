package vn.medora.api.identity;

import org.springframework.data.repository.Repository;
import java.util.UUID;

// Exposes insert only. No audit update/delete API is provided.
public interface IdentityAuditRepository extends Repository<IdentityAuditEvent, UUID> {
    IdentityAuditEvent save(IdentityAuditEvent event);
}
