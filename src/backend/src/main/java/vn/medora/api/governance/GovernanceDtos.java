package vn.medora.api.governance;

import jakarta.validation.constraints.*;
import java.util.UUID;

public final class GovernanceDtos {
    private GovernanceDtos() { }
    public record MergeInput(@NotNull UUID sourceId,@NotNull UUID targetId,@NotBlank @Size(max=2000) String reason) { }
    public record Decision(@NotNull @PositiveOrZero Long version,@NotNull @Pattern(regexp="APPROVE|REJECT") String action,
        @NotNull Boolean identityVerified,@NotBlank @Size(max=2000) String reason) { }
    public record Reason(@NotBlank @Size(max=2000) String reason) { }
    public record DischargeDecision(@NotNull @PositiveOrZero Long version,@NotNull @Pattern(regexp="APPROVE|REJECT") String action,
        @NotBlank @Size(max=2000) String reason) { }
    public record TemplateInput(@NotBlank @Pattern(regexp="[A-Z0-9_-]{1,40}") String code,
        @NotBlank @Size(max=2000) String body,@NotNull Boolean enabled,@PositiveOrZero Long version) { }
    public record NotifyInput(@NotNull UUID templateId,@NotNull UUID appointmentId,@NotNull UUID recipientId) { }
    public record DiagnosisInput(@NotBlank @Pattern(regexp="[A-Z0-9._-]{1,32}") String code,
        @NotBlank @Size(max=200) String name,@NotNull Boolean enabled) { }
}
