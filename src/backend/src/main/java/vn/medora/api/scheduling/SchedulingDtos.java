package vn.medora.api.scheduling;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;

public final class SchedulingDtos {
    private SchedulingDtos() { }
    public record RoomInput(@NotBlank @Pattern(regexp = "[A-Z0-9_-]{1,32}") String code,
        @NotBlank @Size(max = 160) String name, @NotBlank @Size(max = 40) String roomType,
        @NotNull @Size(max = 500) String equipment, boolean enabled, @PositiveOrZero Long version) { }
    public record ServiceInput(@NotBlank @Pattern(regexp = "[A-Z0-9_-]{1,32}") String code,
        @NotBlank @Size(max = 160) String name, @Min(5) @Max(240) int durationMinutes,
        @Min(0) @Max(60) int bufferMinutes, boolean enabled, @PositiveOrZero Long version) { }
    public record ScheduleInput(@NotNull UUID providerId,
        @NotNull @Pattern(regexp = "WORK|ABSENCE") String kind,
        @NotNull Instant startsAt, @NotNull Instant endsAt) { }
    public record CapabilityInput(@NotNull UUID providerId, @NotNull UUID serviceId) { }
    public record BookingInput(@NotNull UUID patientId, @NotNull UUID providerId,
        @NotNull UUID serviceId, @NotNull UUID roomId, @NotNull Instant startsAt) { }
    public record RescheduleInput(@NotNull UUID providerId, @NotNull UUID serviceId,
        @NotNull UUID roomId, @NotNull Instant startsAt,
        @NotNull @PositiveOrZero Long version, @NotBlank @Size(max = 500) String reason) { }
    public record TransitionInput(@NotNull @Pattern(regexp = "CONFIRM|CANCEL|NO_SHOW") String action,
        @NotNull @PositiveOrZero Long version, @Size(max = 500) String reason) { }
    public record CheckInInput(@NotNull UUID patientId, UUID appointmentId,
        @NotNull UUID doctorId, UUID nurseId, boolean identityVerified) { }
}
