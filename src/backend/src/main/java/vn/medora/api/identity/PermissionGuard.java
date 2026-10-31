package vn.medora.api.identity;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import java.util.Set;
import java.util.UUID;

@Component
public class PermissionGuard {
    private final UserAccountRepository accounts;
    private final UserRoleAssignmentRepository assignments;

    public PermissionGuard(UserAccountRepository accounts,
        UserRoleAssignmentRepository assignments) {
        this.accounts = accounts;
        this.assignments = assignments;
    }

    public UserAccount currentActor() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
            || !(authentication.getPrincipal() instanceof AccountPrincipal principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED");
        }
        UserAccount actor = accounts.findById(principal.getAccountId())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED"));
        if (actor.getStatus() != AccountStatus.ACTIVE
            || actor.getAuthVersion() != principal.getAuthVersion()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED");
        }
        return actor;
    }

    public Set<String> permissions(UUID facilityId) {
        UserAccount actor = currentActor();
        return assignments.findPermissionCodes(actor.getId(), facilityId,
            AccountStatus.ACTIVE);
    }

    public UserAccount require(UUID facilityId, String permission) {
        UserAccount actor = currentActor();
        if (!assignments.findPermissionCodes(actor.getId(), facilityId,
            AccountStatus.ACTIVE).contains(permission)) {
            throw new AccessDeniedException("FORBIDDEN");
        }
        return actor;
    }
}
