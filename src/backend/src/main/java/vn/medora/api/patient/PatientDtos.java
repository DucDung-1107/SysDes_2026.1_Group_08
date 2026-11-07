package vn.medora.api.patient;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.UUID;

public final class PatientDtos {
    private PatientDtos() { }
    public record PatientInput(
        @NotBlank @Size(max = 160) String fullName,
        @NotNull @PastOrPresent LocalDate dateOfBirth,
        @NotNull @Pattern(regexp = "MALE|FEMALE|OTHER|UNKNOWN") String sex,
        @Pattern(regexp = "\\+?[0-9]{7,20}") String phone,
        @Size(max = 32) String nationalId,
        @Size(max = 500) String address,
        Boolean duplicateReviewed,
        @Size(max = 300) String duplicateReviewReason,
        @PositiveOrZero Long version
    ) { }
    public record PatientView(UUID id, String code, String fullName, LocalDate dateOfBirth,
        String sex, String phone, String nationalId, String address, Long version, UUID mergedIntoId) {
        static PatientView of(Patient p) {
            return new PatientView(p.getId(), p.code, p.fullName, p.dateOfBirth, p.sex,
                p.phone, p.nationalId, p.address, p.getVersion(), p.getMergedIntoId());
        }
    }
}
