package vn.medora.api.patient;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.*;

public interface PatientRepository extends JpaRepository<Patient, UUID> {
    Optional<Patient> findByIdAndFacilityId(UUID id, UUID facilityId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Patient p where p.id = :id and p.facilityId = :facility")
    Optional<Patient> lock(@Param("id") UUID id, @Param("facility") UUID facility);

    @Query("""
        select p from Patient p where p.facilityId = :facility and p.mergedIntoId is null and
        (p.normalizedName like :q escape '!' or lower(p.code) like :q escape '!'
         or p.phone like :q escape '!' or p.nationalId like :q escape '!')
        """)
    Page<Patient> search(@Param("facility") UUID facility, @Param("q") String q, Pageable page);

    @Query("""
        select p from Patient p where p.facilityId = :facility and
        ((p.normalizedName = :name and p.dateOfBirth = :dob)
         or (:phone is not null and p.phone = :phone)
         or (:nationalId is not null and p.nationalId = :nationalId))
        order by p.code
        """)
    List<Patient> duplicates(@Param("facility") UUID facility, @Param("name") String name,
        @Param("dob") LocalDate dob, @Param("phone") String phone,
        @Param("nationalId") String nationalId, Pageable limit);
}
