package vn.medora.api.identity;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class AccountDtos {
    private AccountDtos() { }

    // Request records override toString so passwords are not accidentally logged.
    public record CreateAccountRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._-]{2,63}$") String username,
        @NotBlank @Size(max = 160) String displayName,
        @NotBlank @Size(min = 12, max = 72) String password,
        @NotEmpty @Size(max = 16) Set<@NotBlank @Size(max = 40) String> roles
    ) {
        @Override public String toString() { return "CreateAccountRequest[REDACTED]"; }
    }

    public record UpdateAccountRequest(
        @NotBlank @Size(max = 160) String displayName,
        @NotNull AccountStatus status,
        @NotNull @PositiveOrZero Long version
    ) { }

    public record ReplaceRolesRequest(
        @NotEmpty @Size(max = 16) Set<@NotBlank @Size(max = 40) String> roles,
        @NotNull @PositiveOrZero Long version
    ) { }

    public record ResetPasswordRequest(
        @NotBlank @Size(min = 12, max = 72) String password,
        @NotNull @PositiveOrZero Long version
    ) {
        @Override public String toString() { return "ResetPasswordRequest[REDACTED]"; }
    }

    public record AccountView(UUID id, String username, String displayName,
        AccountStatus status, Long version, Instant createdAt, Set<String> roles) { }

    public record PageView<T>(List<T> items, int page, int size, long totalElements) { }

    public record RoleView(String code, String name) { }
}
