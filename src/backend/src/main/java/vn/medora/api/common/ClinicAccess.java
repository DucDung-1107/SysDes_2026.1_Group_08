package vn.medora.api.common;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import vn.medora.api.identity.*;
import java.util.*;
import static vn.medora.api.common.ApiSupport.*;

@Component
public class ClinicAccess {
    private final Store store;
    private final PermissionGuard guard;
    private final FacilityRepository facilities;
    public ClinicAccess(Store store, PermissionGuard guard, FacilityRepository facilities) {
        this.store = store; this.guard = guard; this.facilities = facilities;
    }
    public UUID require(UUID facility, String permission) { return guard.require(facility, permission).getId(); }
    public UUID actor() { return guard.currentActor().getId(); }
    public boolean permits(UUID facility, String permission) { return guard.permissions(facility).contains(permission); }
    public void lock(UUID facility) { facilities.lock(facility).orElseThrow(ApiSupport::missing); }
    public Map<String, Object> encounter(UUID facility, UUID id, String permission, boolean lock) {
        UUID actor = require(facility, permission);
        var row = store.one("select * from medora.encounter where id=? and facility_id=?" + (lock ? " for update" : ""), id, facility);
        if (!actor.equals(row.get("doctorId")) && !actor.equals(row.get("nurseId"))) {
            throw new AccessDeniedException("ENCOUNTER_NOT_ASSIGNED");
        }
        return row;
    }
    public Map<String, Object> doctor(UUID facility, UUID id, boolean lock) {
        var row = encounter(facility, id, "clinical.write", lock);
        if (!actor().equals(row.get("doctorId"))) { throw new AccessDeniedException("ASSIGNED_DOCTOR_REQUIRED"); }
        return row;
    }
    public void open(Map<String, Object> encounter) {
        if (encounter.get("status").equals("COMPLETED")) { throw conflict("ENCOUNTER_COMPLETED"); }
    }
    public void staff(UUID facility, UUID id, String role) {
        if (!store.exists("""
            select exists(select 1 from medora.user_role_assignment a join medora.user_account u on u.id=a.user_account_id
            join medora.facility f on f.id=a.facility_id where a.facility_id=? and a.user_account_id=?
            and a.role_code=? and u.status='ACTIVE' and f.enabled)
            """, facility, id, role)) { throw bad("STAFF_NOT_AVAILABLE_IN_FACILITY"); }
    }
    public void activePatient(UUID facility, UUID patient) {
        var row = store.one("select merged_into_id from medora.patient where id=? and facility_id=?", patient, facility);
        if (row.get("mergedIntoId") != null) { throw conflict("PATIENT_MERGED_USE_CANONICAL_RECORD"); }
    }
    public static void state(Map<String, Object> row, String... allowed) {
        if (!Arrays.asList(allowed).contains(row.get("status"))) { throw conflict("INVALID_STATE"); }
    }
    public static void reason(String value) { if (optional(value) == null) { throw bad("REASON_REQUIRED"); } }
}
