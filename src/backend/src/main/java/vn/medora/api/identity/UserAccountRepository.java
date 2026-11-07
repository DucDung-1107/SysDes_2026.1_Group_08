package vn.medora.api.identity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;


public interface UserAccountRepository
    extends JpaRepository<UserAccount,UUID>    {
    Optional<UserAccount> findByUsername(String username);
    boolean existsByUsername(String username);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT u
        FROM UserAccount u
        WHERE u.id = :id
    """)
    Optional<UserAccount> findForUpdate(
        @Param("id") UUID id
    );
    @Query(
        value = """
            SELECT a FROM UserAccount a
            WHERE EXISTS (SELECT 1 FROM UserRoleAssignment u
                          WHERE u.account = a AND u.facility.id = :facilityId)
        """,
        countQuery = """
            SELECT COUNT(a.id) FROM UserAccount a
            WHERE EXISTS (SELECT 1 FROM UserRoleAssignment u
                          WHERE u.account = a AND u.facility.id = :facilityId)
        """
    )
    Page<UserAccount> findInFacility(
        @Param("facilityId") UUID facilityId,
        Pageable pageable
    );
}
