package vn.medora.api.identity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

import java.util.Set;
import java.util.UUID;

public interface UserRoleAssignmentRepository
    extends JpaRepository<UserRoleAssignment,UUID>    {
    @Query("""
        SELECT DISTINCT p.code
        FROM UserRoleAssignment a
        JOIN a.role r
        JOIN r.permissions p
        WHERE a.account.id = :accountId
          AND a.facility.id = :facilityId
          AND a.facility.enabled = true
          AND a.account.status = :status
        """)
    Set<String> findPermissionCodes(
        @Param("accountId") UUID accountId,
        @Param("facilityId") UUID facilityId,
        @Param("status") AccountStatus status
    );
    @Query("""
        SELECT DISTINCT u.facility.id
        FROM UserRoleAssignment u
        WHERE u.account.id = :accountId
    """)
    Set<UUID> findFacilityIds(
    @Param("accountId") UUID accountId
    );
    @Query("""
        SELECT DISTINCT u.role.code
        FROM UserRoleAssignment u
        WHERE u.account.id = :accountId
          AND u.facility.id = :facilityId
    """)
    List<String> findRoleCodes(
    @Param("accountId") UUID accountId,
    @Param("facilityId") UUID facilityId
    );
    List<UserRoleAssignment> findByAccount_IdAndFacility_Id(
        UUID accountId,
        UUID facilityId
    );
    boolean existsByAccount_IdAndFacility_Id(UUID accountId, UUID facilityId);
}
