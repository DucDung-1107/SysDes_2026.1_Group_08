package vn.medora.api.identity;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import java.util.stream.Collectors;
import static vn.medora.api.identity.AccountDtos.*;

@Service
@Transactional
public class AccountAdminService {
    private final UserAccountRepository accounts;
    private final UserRoleAssignmentRepository assignments;
    private final FacilityRepository facilities;
    private final RoleRepository roles;
    private final PermissionGuard guard;
    private final PasswordPolicy passwords;
    private final IdentityAuditRepository audit;

    public AccountAdminService(UserAccountRepository accounts,
        UserRoleAssignmentRepository assignments, FacilityRepository facilities,
        RoleRepository roles, PermissionGuard guard, PasswordPolicy passwords,
        IdentityAuditRepository audit) {
        this.accounts = accounts;
        this.assignments = assignments;
        this.facilities = facilities;
        this.roles = roles;
        this.guard = guard;
        this.passwords = passwords;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public PageView<AccountView> list(UUID facilityId, int page, int size) {
        guard.require(facilityId, "account.read");
        if (page < 0 || size < 1 || size > 100 || (long) page * size > 1_000_000) {
            throw badRequest("INVALID_PAGINATION");
        }
        var result = accounts.findInFacility(facilityId,
            PageRequest.of(page, size, Sort.by("username")));
        return new PageView<>(result.getContent().stream()
            .map(account -> view(account, facilityId)).toList(),
            page, size, result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public AccountView get(UUID facilityId, UUID accountId) {
        guard.require(facilityId, "account.read");
        requireMembership(accountId, facilityId);
        return view(accounts.findById(accountId).orElseThrow(this::notFound), facilityId);
    }

    @Transactional(readOnly = true)
    public List<RoleView> listRoles(UUID facilityId) {
        guard.require(facilityId, "account.assign_role");
        return roles.findAll().stream().map(role -> new RoleView(role.getCode(),
            role.getName())).sorted(Comparator.comparing(RoleView::code)).toList();
    }

    public AccountView create(UUID facilityId, CreateAccountRequest request) {
        UserAccount actor = guard.require(facilityId, "account.create");
        guard.require(facilityId, "account.assign_role");
        Facility facility = facilities.findById(facilityId).orElseThrow(this::notFound);
        String username = request.username().strip().toLowerCase(Locale.ROOT);
        if (accounts.existsByUsername(username)) {
            throw conflict("ACCOUNT_ALREADY_EXISTS");
        }
        List<Role> selectedRoles = resolveRoles(request.roles());
        var account = accounts.saveAndFlush(new UserAccount(username,
            request.displayName().strip(), passwords.encode(request.password())));
        for (Role role : selectedRoles) {
            assignments.save(new UserRoleAssignment(account, role, facility));
        }
        assignments.flush();
        record(actor.getId(), account.getId(), facilityId, "ACCOUNT_CREATED",
            "roles=" + sortedCodes(request.roles()));
        return view(account, facilityId);
    }

    public AccountView update(UUID facilityId, UUID accountId,
        UpdateAccountRequest request) {
        UserAccount actor = guard.require(facilityId, "account.update");
        UserAccount target = lockTarget(actor.getId(), accountId, facilityId);
        checkVersion(target, request.version());
        // Profile/status belongs to the global account: require control of ALL scopes.
        Set<UUID> scopes = requireAllScopes(accountId);
        AccountStatus previousStatus = target.getStatus();
        target.updateProfile(request.displayName(), request.status());
        accounts.flush();
        for (UUID scope : scopes) {
            record(actor.getId(), accountId, scope, "ACCOUNT_UPDATED",
                "status=" + previousStatus + "->" + target.getStatus());
        }
        return view(target, facilityId);
    }

    public AccountView replaceRoles(UUID facilityId, UUID accountId,
        ReplaceRolesRequest request) {
        UserAccount actor = guard.require(facilityId, "account.assign_role");
        UserAccount target = lockTarget(actor.getId(), accountId, facilityId);
        checkVersion(target, request.version());
        List<Role> selectedRoles = resolveRoles(request.roles());
        List<UserRoleAssignment> existing = assignments
            .findByAccount_IdAndFacility_Id(accountId, facilityId);
        Set<String> previous = existing.stream().map(a -> a.getRole().getCode())
            .collect(Collectors.toSet());
        if (previous.equals(request.roles())) { return view(target, facilityId); }
        for (UserRoleAssignment assignment : existing) {
            if (!request.roles().contains(assignment.getRole().getCode())) {
                assignments.delete(assignment);
            }
        }
        assignments.flush();
        Facility facility = facilities.findById(facilityId).orElseThrow(this::notFound);
        for (Role role : selectedRoles) {
            if (!previous.contains(role.getCode())) {
                assignments.save(new UserRoleAssignment(target, role, facility));
            }
        }
        assignments.flush();
        target.invalidateSessions();
        accounts.flush();
        record(actor.getId(), accountId, facilityId, "ACCOUNT_ROLES_REPLACED",
            "before=" + sortedCodes(previous) + ";after=" + sortedCodes(request.roles()));
        return view(target, facilityId);
    }

    public AccountView resetPassword(UUID facilityId, UUID accountId,
        ResetPasswordRequest request) {
        UserAccount actor = guard.require(facilityId, "account.update");
        UserAccount target = lockTarget(actor.getId(), accountId, facilityId);
        checkVersion(target, request.version());
        Set<UUID> scopes = requireAllScopes(accountId);
        target.resetPassword(passwords.encode(request.password()));
        accounts.flush();
        for (UUID scope : scopes) {
            record(actor.getId(), accountId, scope, "ACCOUNT_PASSWORD_RESET",
                "sessions_invalidated");
        }
        return view(target, facilityId);
    }
    public AccountView revokeSessions(UUID facilityId, UUID accountId, Long version) {
        UserAccount actor = guard.require(facilityId, "account.update");
        UserAccount target = lockTarget(actor.getId(), accountId, facilityId);
        checkVersion(target, version);
        Set<UUID> scopes = requireAllScopes(accountId);
        target.invalidateSessions(); accounts.flush();
        for (UUID scope : scopes) { record(actor.getId(), accountId, scope, "ACCOUNT_SESSIONS_REVOKED", "sessions_invalidated"); }
        return view(target, facilityId);
    }

    private UserAccount lockTarget(UUID actorId, UUID accountId, UUID facilityId) {
        if (actorId.equals(accountId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "SELF_ADMINISTRATION_NOT_ALLOWED");
        }
        requireMembership(accountId, facilityId);
        return accounts.findForUpdate(accountId).orElseThrow(this::notFound);
    }

    private void requireMembership(UUID accountId, UUID facilityId) {
        if (!assignments.existsByAccount_IdAndFacility_Id(accountId, facilityId)) {
            throw notFound();
        }
    }

    private Set<UUID> requireAllScopes(UUID accountId) {
        Set<UUID> scopes = assignments.findFacilityIds(accountId);
        for (UUID scope : scopes) { guard.require(scope, "account.update"); }
        return scopes;
    }

    private List<Role> resolveRoles(Set<String> codes) {
        if (codes == null || codes.isEmpty() || codes.size() > 16) {
            throw badRequest("INVALID_ROLES");
        }
        List<Role> found = roles.findAllById(codes);
        if (found.size() != codes.size()) { throw badRequest("UNKNOWN_ROLE"); }
        return found;
    }

    private void checkVersion(UserAccount target, Long expected) {
        if (!Objects.equals(target.getVersion(), expected)) {
            throw conflict("STALE_VERSION");
        }
    }

    private AccountView view(UserAccount account, UUID facilityId) {
        Set<String> codes = assignments.findByAccount_IdAndFacility_Id(
            account.getId(), facilityId).stream().map(a -> a.getRole().getCode())
            .collect(Collectors.toCollection(TreeSet::new));
        return new AccountView(account.getId(), account.getUsername(),
            account.getDisplayName(), account.getStatus(), account.getVersion(),
            account.getCreatedAt(), codes);
    }

    private void record(UUID actor, UUID target, UUID facility, String action,
        String detail) {
        audit.save(new IdentityAuditEvent(actor, target, facility, action, detail));
    }

    private String sortedCodes(Set<String> codes) {
        return String.join(",", new TreeSet<>(codes));
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND");
    }

    private ResponseStatusException badRequest(String code) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, code);
    }

    private ResponseStatusException conflict(String code) {
        return new ResponseStatusException(HttpStatus.CONFLICT, code);
    }
}
