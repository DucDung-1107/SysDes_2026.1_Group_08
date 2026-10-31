package vn.medora.api.identity;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// Development only: explicitly enable profile "dev". Never resets an existing account.
@Component
@Profile("dev")
public class DevBootstrap implements ApplicationRunner {
    private final UserAccountRepository accounts;
    private final FacilityRepository facilities;
    private final RoleRepository roles;
    private final UserRoleAssignmentRepository assignments;
    private final PasswordPolicy passwords;
    private final IdentityAuditRepository audit;
    private final String initialPassword;

    public DevBootstrap(UserAccountRepository accounts, FacilityRepository facilities,
        RoleRepository roles, UserRoleAssignmentRepository assignments,
        PasswordPolicy passwords, IdentityAuditRepository audit,
        @Value("${MEDORA_BOOTSTRAP_PASSWORD:}") String initialPassword) {
        this.accounts = accounts;
        this.facilities = facilities;
        this.roles = roles;
        this.assignments = assignments;
        this.passwords = passwords;
        this.audit = audit;
        this.initialPassword = initialPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        if (accounts.count() != 0) { return; }
        if (initialPassword.isBlank()) {
            throw new IllegalStateException("Set MEDORA_BOOTSTRAP_PASSWORD before enabling the dev profile");
        }
        Facility facility = facilities.findByCode("DEMO").orElseGet(() ->
            facilities.save(new Facility("DEMO", "MEDORA Demo Facility")));
        var administratorRole = roles.findById("SYSTEM_ADMINISTRATOR").orElseThrow();
        var administrator = accounts.saveAndFlush(new UserAccount("admin.demo",
            "Demo Administrator", passwords.encode(initialPassword)));
        assignments.save(new UserRoleAssignment(administrator, administratorRole, facility));
        audit.save(new IdentityAuditEvent(administrator.getId(), administrator.getId(),
            facility.getId(), "BOOTSTRAP_ADMIN", "development_profile_only"));
    }
}
