package vn.medora.api.identity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;


public interface FacilityRepository
    extends JpaRepository<Facility,UUID>    {
        Optional<Facility> findByCode(String code);
        @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
        @org.springframework.data.jpa.repository.Query("select f from Facility f where f.id = :id")
        Optional<Facility> lock(@org.springframework.data.repository.query.Param("id") UUID id);
}
