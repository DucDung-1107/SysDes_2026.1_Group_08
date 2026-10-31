package vn.medora.api.identity;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
import static vn.medora.api.identity.AccountDtos.*;

@RestController
@RequestMapping("/api/admin/facilities/{facilityId}")
public class AccountAdminController {
    private final AccountAdminService service;

    public AccountAdminController(AccountAdminService service) { this.service = service; }
    @PostMapping("/accounts/{accountId}/revoke-sessions")
    public AccountView revoke(@PathVariable UUID facilityId, @PathVariable UUID accountId,
        @Valid @RequestBody vn.medora.api.common.VersionRequest request) {
        return service.revokeSessions(facilityId, accountId, request.version());
    }

    @GetMapping("/accounts")
    public PageView<AccountView> list(@PathVariable UUID facilityId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
        return service.list(facilityId, page, size);
    }

    @GetMapping("/accounts/{accountId}")
    public AccountView get(@PathVariable UUID facilityId, @PathVariable UUID accountId) {
        return service.get(facilityId, accountId);
    }

    @GetMapping("/roles")
    public List<RoleView> roles(@PathVariable UUID facilityId) {
        return service.listRoles(facilityId);
    }

    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountView create(@PathVariable UUID facilityId,
        @Valid @RequestBody CreateAccountRequest request) {
        return service.create(facilityId, request);
    }

    @PutMapping("/accounts/{accountId}")
    public AccountView update(@PathVariable UUID facilityId, @PathVariable UUID accountId,
        @Valid @RequestBody UpdateAccountRequest request) {
        return service.update(facilityId, accountId, request);
    }

    @PutMapping("/accounts/{accountId}/roles")
    public AccountView replaceRoles(@PathVariable UUID facilityId,
        @PathVariable UUID accountId, @Valid @RequestBody ReplaceRolesRequest request) {
        return service.replaceRoles(facilityId, accountId, request);
    }

    @PutMapping("/accounts/{accountId}/password")
    public AccountView resetPassword(@PathVariable UUID facilityId,
        @PathVariable UUID accountId, @Valid @RequestBody ResetPasswordRequest request) {
        return service.resetPassword(facilityId, accountId, request);
    }
}
