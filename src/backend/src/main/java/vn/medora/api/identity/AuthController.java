package vn.medora.api.identity;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final PermissionGuard guard;
    private final UserRoleAssignmentRepository assignments;

    public AuthController(PermissionGuard guard,
        UserRoleAssignmentRepository assignments) {
        this.guard = guard;
        this.assignments = assignments;
    }

    public record CsrfView(String headerName, String token) { }
    public record MeView(UUID id, String username, String displayName,
                         Set<UUID> facilityIds) { }

    @GetMapping("/csrf")
    public CsrfView csrf(CsrfToken token) {
        return new CsrfView(token.getHeaderName(), token.getToken());
    }

    @GetMapping("/me")
    @Transactional(readOnly = true)
    public MeView me() {
        UserAccount account = guard.currentActor();
        return new MeView(account.getId(), account.getUsername(),
            account.getDisplayName(), assignments.findFacilityIds(account.getId()));
    }

    @GetMapping("/permissions")
    @Transactional(readOnly = true)
    public Set<String> permissions(@RequestParam UUID facilityId) {
        return guard.permissions(facilityId);
    }
}
