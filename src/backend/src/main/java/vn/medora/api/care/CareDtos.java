package vn.medora.api.care;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

public final class CareDtos {
    private CareDtos() { }
    public record MedicationInput(@NotBlank @Pattern(regexp="[A-Z0-9._-]{1,40}") String code,
        @NotBlank @Size(max=160) String name, @NotBlank @Pattern(regexp="[A-Z0-9._-]{1,80}") String ingredientCode,
        @NotBlank @Size(max=40) String unit, @NotNull Boolean enabled, @PositiveOrZero Long version) { }
    public record RuleInput(@NotNull UUID medicationId, UUID otherMedicationId,
        @NotNull @Pattern(regexp="INTERACTION|CONTRAINDICATION") String kind,
        @Pattern(regexp="[A-Z0-9._-]{1,80}") String conditionCode,
        @NotNull @Pattern(regexp="WARN|BLOCK") String severity, @NotNull Boolean overridable,
        @NotBlank @Size(max=500) String message, @NotBlank @Size(max=500) String source,
        @NotNull Boolean enabled, @PositiveOrZero Long version) { }
    public record SafetyPolicyInput(@NotBlank @Size(max=500) String source,
        @NotBlank @Size(max=80) String dataVersion, @NotNull Boolean enabled, @PositiveOrZero Long version) { }
    public record ProfileInput(@NotNull @Pattern(regexp="UNKNOWN|KNOWN_NONE|RECORDED") String allergyStatus,
        @NotNull @Size(max=40) Set<@Pattern(regexp="[A-Z0-9._-]{1,80}") String> allergyCodes,
        @NotNull @Size(max=40) Set<@Pattern(regexp="[A-Z0-9._-]{1,80}") String> conditionCodes,
        @NotNull @Size(max=40) Set<UUID> currentMedicationIds, @PositiveOrZero Long version) { }
    public record RxItem(@NotNull UUID medicationId, @NotBlank @Size(max=160) String dose,
        @NotBlank @Size(max=80) String route, @NotBlank @Size(max=160) String frequency,
        @Min(1) @Max(365) int durationDays, @NotNull @DecimalMin("0.001") @Digits(integer=9,fraction=3) BigDecimal quantity,
        @NotBlank @Size(max=2000) String instructions) { }
    public record RxInput(@NotEmpty @Size(max=40) List<@Valid RxItem> items, @PositiveOrZero Long version) { }
    public record IssueInput(@NotNull @PositiveOrZero Long version, @NotNull UUID pharmacistId,
        @NotNull @Size(max=100) Set<@Size(max=100) String> acknowledgedWarnings,
        @Size(max=500) String overrideReason) { }
    public record ReasonVersion(@NotNull @PositiveOrZero Long version, @NotBlank @Size(max=500) String reason) { }
    public record MedicineEventInput(@NotNull UUID itemId, @NotNull @Pattern(regexp="DISPENSED|ADMINISTERED") String kind,
        @NotNull @DecimalMin("0.001") @Digits(integer=9,fraction=3) BigDecimal quantity,
        @NotNull @PastOrPresent Instant performedAt) { }
    public record RequirementsInput(@Size(max=40) String requiredRoomType,
        @NotNull @Size(max=500) String requiredEquipment, @NotNull @PositiveOrZero Long version) { }
    public record OrderInput(@NotNull UUID serviceId, @NotBlank @Size(max=2000) String reason,
        @NotNull @Pattern(regexp="ROUTINE|URGENT") String priority, @NotNull @Size(max=2000) String preparation,
        @NotNull Boolean required, @NotNull @Pattern(regexp="PROCEDURE|LAB|IMAGING") String kind) { }
    public record AllocationInput(@NotNull UUID staffId, @NotNull UUID roomId, @NotNull Instant startsAt,
        @NotNull @PositiveOrZero Long version, @NotBlank @Size(max=2000) String reason) { }
    public record OrderAction(@NotNull @Pattern(regexp="START|DELAY|COMPLETE|CANCEL|NOT_PERFORMED") String action,
        @NotNull @PositiveOrZero Long version, @Size(max=10000) String result, @Size(max=2000) String reason) { }
    public record ServiceSelection(@NotNull UUID serviceId) { }
}
