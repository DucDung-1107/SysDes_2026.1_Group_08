package vn.medora.api.identity;

import org.springframework.security.core.userdetails.User;
import java.io.Serial;
import java.util.List;
import java.util.UUID;

// Permissions are loaded per facility in PermissionGuard, never globally here.
public final class AccountPrincipal extends User {
    @Serial
    private static final long serialVersionUID = 1L;
    private final UUID accountId;
    private final long authVersion;

    public AccountPrincipal(UserAccount account) {
        super(account.getUsername(), account.getPasswordHash(),
            account.getStatus() != AccountStatus.DISABLED,
            true, true, account.getStatus() != AccountStatus.LOCKED, List.of());
        this.accountId = account.getId();
        this.authVersion = account.getAuthVersion();
    }

    public UUID getAccountId() { return accountId; }
    public long getAuthVersion() { return authVersion; }
}
