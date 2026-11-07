package vn.medora.api.patient;

import jakarta.persistence.*;
import vn.medora.api.common.ScopedEntity;
import java.time.LocalDate;
import java.util.UUID;
import static vn.medora.api.common.ApiSupport.*;

@Entity @Table(name = "patient", schema = "medora")
public class Patient extends ScopedEntity {
    @Column(nullable = false, unique = true, length = 40, updatable = false)
    String code;
    @Column(name = "full_name", nullable = false, length = 160)
    String fullName;
    @Column(name = "normalized_name", nullable = false, length = 160)
    String normalizedName;
    @Column(name = "date_of_birth", nullable = false)
    LocalDate dateOfBirth;
    @Column(nullable = false, length = 16)
    String sex;
    @Column(length = 24) String phone;
    @Column(name = "national_id", length = 32) String nationalId;
    @Column(length = 500) String address;
    @Column(name = "merged_into_id") UUID mergedIntoId;
    public UUID getMergedIntoId() { return mergedIntoId; }

    protected Patient() { }
    Patient(UUID facility, PatientDtos.PatientInput request) {
        facilityId = facility;
        code = "PT-" + UUID.randomUUID();
        update(request);
    }
    void update(PatientDtos.PatientInput request) {
        fullName = request.fullName().strip();
        normalizedName = normalized(fullName);
        dateOfBirth = request.dateOfBirth();
        sex = request.sex();
        phone = optional(request.phone());
        nationalId = optional(request.nationalId());
        address = optional(request.address());
    }
}
