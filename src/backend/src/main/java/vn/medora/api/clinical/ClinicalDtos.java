package vn.medora.api.clinical;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class ClinicalDtos {
    private ClinicalDtos() { }
    public record HistoryInput(@NotNull @Pattern(regexp = "ALLERGY|HISTORY|MEDICATION") String kind,
        @NotBlank @Size(max = 4000) String content, UUID supersedesId,
        @Size(max = 500) String correctionReason) { }
    public record VitalInput(
        @DecimalMin("25") @DecimalMax("45") BigDecimal temperatureC,
        @Min(20) @Max(300) Integer pulseBpm,
        @Min(30) @Max(300) Integer systolicMmhg,
        @Min(10) @Max(200) Integer diastolicMmhg,
        @Min(1) @Max(100) Integer respiratoryRate,
        @DecimalMin("0") @DecimalMax("100") BigDecimal spo2Percent,
        @DecimalMin(value = "0", inclusive = false) @DecimalMax("9999") BigDecimal weightKg,
        @DecimalMin(value = "0", inclusive = false) @DecimalMax("999") BigDecimal heightCm,
        @Size(max = 4000) String screening,
        @NotNull @PastOrPresent Instant measuredAt, boolean verified,
        UUID supersedesId, @Size(max = 500) String correctionReason) { }
    public record DiagnosisInput(@NotBlank @Size(max = 32) String code,
        @NotBlank @Size(max = 4000) String symptoms) { }
    public record NoteInput(@NotNull @Size(max = 10000) String subjective,
        @NotNull @Size(max = 10000) String objective,
        @NotNull @Size(max = 10000) String assessment,
        @NotNull @Size(max = 10000) String plan,
        @PositiveOrZero Long version) { }
    public record AmendmentInput(@NotBlank @Size(max = 500) String reason,
        @NotNull @PositiveOrZero Long version) { }
    public record CloseInput(@NotNull @PositiveOrZero Long version,
        @NotBlank @Size(max = 10000) String summary,
        @NotBlank @Size(max = 10000) String instructions,
        @NotBlank @Size(max = 2000) String followUp) { }
    public record AssignmentInput(UUID nurseId, @NotNull @PositiveOrZero Long version) { }
}
