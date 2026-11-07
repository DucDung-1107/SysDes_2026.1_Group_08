package vn.medora.api.clinical;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.medora.api.common.*;
import vn.medora.api.identity.*;
import vn.medora.api.patient.PatientRepository;
import vn.medora.api.scheduling.*;
import java.time.*;
import java.util.*;
import static vn.medora.api.common.ApiSupport.*;
import static vn.medora.api.common.SqlRows.*;
import static vn.medora.api.scheduling.SchedulingService.expected;
import static vn.medora.api.scheduling.SchedulingDtos.CheckInInput;
import static vn.medora.api.clinical.ClinicalDtos.*;

@Service @Transactional
public class ClinicalService {
    private final ClinicalRepository repository;
    private final SchedulingRepository appointments;
    private final SchedulingService scheduling;
    private final PatientRepository patients;
    private final PermissionGuard guard;
    private final AuditService audit;
    private final Store db;
    private final ZoneId zone;
    private final boolean walkInEnabled;
    public ClinicalService(ClinicalRepository repository, SchedulingRepository appointments,
        SchedulingService scheduling, PatientRepository patients, PermissionGuard guard,
        AuditService audit, Store db, @Value("${medora.facility-zone:Asia/Ho_Chi_Minh}") String zone,
        @Value("${medora.walk-in-enabled:true}") boolean walkInEnabled) {
        this.repository = repository; this.appointments = appointments; this.scheduling = scheduling;
        this.patients = patients; this.guard = guard; this.audit = audit; this.db = db;
        this.zone = ZoneId.of(zone); this.walkInEnabled = walkInEnabled;
    }
    public Map<String, Object> checkIn(UUID facility, CheckInInput r) {
        var actor = guard.require(facility, "appointment.write"); scheduling.lockClinic(facility);
        if (!r.identityVerified()) { throw bad("PATIENT_IDENTITY_REQUIRES_VERIFICATION"); }
        if (db.exists("select exists(select 1 from medora.patient where id=? and facility_id=? and merged_into_id is not null)", r.patientId(), facility)) {
            throw conflict("PATIENT_MERGED_USE_CANONICAL_RECORD");
        }
        patients.lock(r.patientId(), facility).orElseThrow(ApiSupport::missing);
        scheduling.requireStaff(facility, r.doctorId(), "DOCTOR");
        if (r.nurseId() != null) { scheduling.requireStaff(facility, r.nurseId(), "NURSE"); }
        if (repository.activeEncounter(facility, r.patientId())) { throw conflict("ACTIVE_ENCOUNTER_EXISTS"); }
        if (r.appointmentId() == null) {
            if (!walkInEnabled) { throw conflict("WALK_IN_DISABLED"); }
        } else {
            var appointment = appointments.appointment(facility, r.appointmentId());
            if (!r.patientId().equals(appointment.get("patientId")) || !r.doctorId().equals(appointment.get("providerId"))) {
                throw bad("APPOINTMENT_IDENTITY_MISMATCH");
            }
            if (!Set.of("BOOKED", "CONFIRMED").contains(appointment.get("status"))) {
                throw conflict("INVALID_APPOINTMENT_STATE");
            }
            if (!instant(appointment, "startsAt").atZone(zone).toLocalDate().equals(LocalDate.now(zone))) {
                throw conflict("CHECK_IN_REQUIRES_APPOINTMENT_DATE");
            }
            appointments.transition(facility, r.appointmentId(), "CHECKED_IN");
            appointments.event(facility, r.appointmentId(), actor.getId(), "CHECKED_IN", null);
        }
        var result = repository.checkIn(facility, r.patientId(), r.appointmentId(), r.doctorId(), r.nurseId());
        audit.record(actor.getId(), facility, "PATIENT_CHECKED_IN", "ENCOUNTER", uuid(result, "id"));
        return result;
    }
    public List<Map<String, Object>> queue(UUID facility, int page, int size) {
        var actor = guard.require(facility, "queue.read"); page(page, size, "id");
        return repository.queue(facility, actor.getId(), guard.permissions(facility).contains("appointment.write"), page, size);
    }
    public Map<String, Object> assignNurse(UUID facility, UUID id, AssignmentInput r) {
        var actor = guard.require(facility, "appointment.write"); scheduling.lockClinic(facility);
        var encounter = repository.encounter(facility, id, true); expected(encounter, r.version()); open(encounter);
        if (r.nurseId() != null) { scheduling.requireStaff(facility, r.nurseId(), "NURSE"); }
        repository.assignNurse(id, r.nurseId());
        audit.record(actor.getId(), facility, "ENCOUNTER_NURSE_ASSIGNED", "ENCOUNTER", id);
        // Reception sees operational metadata only.
        var result = new LinkedHashMap<>(repository.encounter(facility, id, false));
        result.remove("summary"); result.remove("instructions"); result.remove("followUp");
        return result;
    }
    public Map<String, Object> get(UUID facility, UUID id) {
        var encounter = access(facility, id, "clinical.read", false);
        record(facility, id, "ENCOUNTER_READ"); return encounter;
    }
    public Map<String, Object> start(UUID facility, UUID id, Long version) {
        var encounter = access(facility, id, "clinical.write", true); expected(encounter, version);
        requireState(encounter, "WAITING"); repository.state(id, "IN_CONSULTATION");
        record(facility, id, "CONSULTATION_STARTED"); return repository.encounter(facility, id, false);
    }
    public List<Map<String, Object>> timeline(UUID facility, UUID id, int page, int size) {
        var encounter = access(facility, id, "clinical.read", false); open(encounter); page(page, size, "id");
        record(facility, id, "PATIENT_TIMELINE_READ");
        return repository.timeline(facility, uuid(encounter, "patientId"), page, size);
    }
    public Map<String, Object> pastVisit(UUID facility, UUID context, UUID visit) {
        var encounter = access(facility, context, "clinical.read", false); open(encounter);
        var historical = repository.encounter(facility, visit, false);
        if (!db.exists("""
            with recursive tree as (select id from medora.patient where id=? and facility_id=? union all
            select p.id from medora.patient p join tree t on p.merged_into_id=t.id where p.facility_id=?)
            select exists(select 1 from tree where id=?)
            """, encounter.get("patientId"), facility, facility, historical.get("patientId"))) { throw missing(); }
        record(facility, visit, "LONGITUDINAL_VISIT_READ");
        return Map.of("encounter", historical, "notes", repository.notes(facility, visit, 0, 100),
            "vitals", repository.vitals(facility, visit, 0, 100), "diagnoses", repository.diagnoses(facility, visit));
    }
    public List<Map<String, Object>> history(UUID facility, UUID id, int page, int size) {
        var encounter = access(facility, id, "clinical.read", false); open(encounter); page(page, size, "id");
        record(facility, id, "PATIENT_HISTORY_READ");
        return repository.history(facility, uuid(encounter, "patientId"), page, size);
    }
    public Map<String, Object> addHistory(UUID facility, UUID id, HistoryInput r) {
        var encounter = access(facility, id, "history.write", true); open(encounter);
        if (r.supersedesId() != null) {
            reason(r.correctionReason());
            var previous = repository.historyEntry(facility, r.supersedesId());
            if (!encounter.get("patientId").equals(previous.get("patientId")) || !r.kind().equals(previous.get("kind"))) {
                throw bad("INVALID_HISTORY_PREDECESSOR");
            }
        }
        var result = repository.addHistory(facility, id, uuid(encounter, "patientId"), guard.currentActor().getId(), r);
        record(facility, id, "PATIENT_HISTORY_RECORDED"); return result;
    }
    public List<Map<String, Object>> vitals(UUID facility, UUID id, int page, int size) {
        access(facility, id, "clinical.read", false); page(page, size, "id");
        record(facility, id, "VITAL_SIGNS_READ"); return repository.vitals(facility, id, page, size);
    }
    public Map<String, Object> addVitals(UUID facility, UUID id, VitalInput r) {
        var encounter = access(facility, id, "screening.write", true); open(encounter);
        if (r.temperatureC() == null && r.pulseBpm() == null && r.systolicMmhg() == null && r.diastolicMmhg() == null
            && r.respiratoryRate() == null && r.spo2Percent() == null && r.weightKg() == null && r.heightCm() == null) {
            throw bad("AT_LEAST_ONE_VITAL_REQUIRED");
        }
        if (r.systolicMmhg() != null && r.diastolicMmhg() != null && r.systolicMmhg() <= r.diastolicMmhg()) {
            throw bad("INVALID_BLOOD_PRESSURE");
        }
        if (r.measuredAt().isBefore(instant(encounter, "createdAt").minusSeconds(300))) {
            throw bad("MEASUREMENT_PREDATES_ENCOUNTER");
        }
        if (r.supersedesId() != null) {
            reason(r.correctionReason());
            if (!id.equals(repository.vital(facility, r.supersedesId()).get("encounterId"))) {
                throw bad("INVALID_VITAL_PREDECESSOR");
            }
        }
        var result = repository.addVitals(facility, id, guard.currentActor().getId(), r);
        record(facility, id, "VITAL_SIGNS_RECORDED"); return result;
    }
    public List<Map<String, Object>> diagnosisCatalog(UUID facility) {
        guard.require(facility, "clinical.read"); return repository.diagnosisCatalog(facility);
    }
    public List<Map<String, Object>> diagnoses(UUID facility, UUID id) {
        access(facility, id, "clinical.read", false); record(facility, id, "DIAGNOSES_READ");
        return repository.diagnoses(facility, id);
    }
    public Map<String, Object> addDiagnosis(UUID facility, UUID id, DiagnosisInput r) {
        var encounter = access(facility, id, "clinical.write", true); requireState(encounter, "IN_CONSULTATION");
        var result = repository.addDiagnosis(facility, id, guard.currentActor().getId(), r);
        record(facility, id, "DIAGNOSIS_RECORDED"); return result;
    }
    public List<Map<String, Object>> notes(UUID facility, UUID id, int page, int size) {
        access(facility, id, "clinical.read", false); page(page, size, "id"); record(facility, id, "CLINICAL_NOTES_READ");
        return repository.notes(facility, id, page, size);
    }
    public Map<String, Object> draft(UUID facility, UUID id, NoteInput r) {
        var encounter = access(facility, id, "clinical.write", true); requireState(encounter, "IN_CONSULTATION");
        if (repository.hasDraft(id) || repository.latestSigned(id).isPresent()) { throw conflict("USE_EXISTING_DRAFT_OR_AMENDMENT"); }
        var result = repository.newDraft(facility, id, null, null, r);
        recordEntry(facility, id, uuid(result, "id"), "NOTE_DRAFT_CREATED"); return result;
    }
    public Map<String, Object> updateDraft(UUID facility, UUID id, UUID note, NoteInput r) {
        var encounter = access(facility, id, "clinical.write", true); requireState(encounter, "IN_CONSULTATION");
        var previous = repository.note(facility, id, note); expected(previous, r.version()); requireDraft(previous);
        var result = repository.updateDraft(facility, id, note, r);
        recordEntry(facility, id, note, "NOTE_DRAFT_SAVED"); return result;
    }
    public Map<String, Object> sign(UUID facility, UUID id, UUID note, Long version) {
        var encounter = access(facility, id, "clinical.write", true); requireState(encounter, "IN_CONSULTATION");
        var previous = repository.note(facility, id, note); expected(previous, version); requireDraft(previous);
        for (String field : List.of("subjective", "objective", "assessment", "plan")) {
            if (previous.get(field).toString().isBlank()) { throw conflict("REQUIRED_NOTE_SECTIONS_MISSING"); }
        }
        if (repository.diagnoses(facility, id).isEmpty()) { throw conflict("DIAGNOSIS_REQUIRED"); }
        var result = repository.sign(facility, id, note, guard.currentActor().getId(), previous.get("status").equals("AMENDMENT_DRAFT"));
        recordEntry(facility, id, note, "CLINICAL_NOTE_SIGNED"); return result;
    }
    public Map<String, Object> amend(UUID facility, UUID id, UUID note, AmendmentInput r) {
        var encounter = access(facility, id, "clinical.write", true); requireState(encounter, "IN_CONSULTATION");
        var previous = repository.note(facility, id, note); expected(previous, r.version()); reason(r.reason());
        var latest = repository.latestSigned(id).orElseThrow(() -> conflict("SIGNED_NOTE_REQUIRED"));
        if (!note.equals(latest.get("id")) || repository.hasDraft(id)) { throw conflict("LATEST_SIGNED_NOTE_REQUIRED"); }
        var content = new NoteInput((String) previous.get("subjective"), (String) previous.get("objective"),
            (String) previous.get("assessment"), (String) previous.get("plan"), null);
        var result = repository.newDraft(facility, id, note, r.reason().strip(), content);
        recordEntry(facility, id, uuid(result, "id"), "NOTE_AMENDMENT_CREATED"); return result;
    }
    public void abandon(UUID facility, UUID id, UUID note, Long version) {
        var encounter = access(facility, id, "clinical.write", true); requireState(encounter, "IN_CONSULTATION");
        var previous = repository.note(facility, id, note); expected(previous, version);
        if (!previous.get("status").equals("AMENDMENT_DRAFT")) { throw conflict("AMENDMENT_DRAFT_REQUIRED"); }
        repository.abandon(note); recordEntry(facility, id, note, "NOTE_AMENDMENT_ABANDONED");
    }
    public Map<String, Object> submit(UUID facility, UUID id, Long version) {
        var encounter = access(facility, id, "clinical.write", true); expected(encounter, version);
        requireState(encounter, "IN_CONSULTATION"); checklist(facility, id);
        repository.state(id, "AWAITING_CLOSURE"); record(facility, id, "ENCOUNTER_SUBMITTED_FOR_CLOSURE");
        return repository.encounter(facility, id, false);
    }
    public Map<String, Object> returnForCorrection(UUID facility, UUID id, Long version) {
        var encounter = access(facility, id, "clinical.write", true); expected(encounter, version);
        requireState(encounter, "AWAITING_CLOSURE"); repository.state(id, "IN_CONSULTATION");
        record(facility, id, "ENCOUNTER_RETURNED_FOR_CORRECTION"); return repository.encounter(facility, id, false);
    }
    public Map<String, Object> complete(UUID facility, UUID id, CloseInput r) {
        var encounter = access(facility, id, "clinical.write", true); expected(encounter, r.version());
        requireState(encounter, "AWAITING_CLOSURE"); checklist(facility, id);
        repository.complete(id, guard.currentActor().getId(), r);
        record(facility, id, "ENCOUNTER_COMPLETED"); return repository.encounter(facility, id, false);
    }
    private void checklist(UUID facility, UUID encounter) {
        if (repository.hasDraft(encounter) || repository.latestSigned(encounter).isEmpty()
            || repository.diagnoses(facility, encounter).isEmpty() || !repository.verifiedVitals(encounter)) {
            throw conflict("CLOSURE_CHECKLIST_INCOMPLETE");
        }
        if (db.exists("select exists(select 1 from medora.prescription where encounter_id=? and status='DRAFT')", encounter)
            || db.exists("select exists(select 1 from medora.procedure_order where encounter_id=? and (status not in ('COMPLETED','CANCELED','NOT_PERFORMED') or reviewed_at is null))", encounter)) {
            throw conflict("CLINICAL_WORK_REMAINS_UNRESOLVED");
        }
        if (db.exists("select exists(select 1 from medora.clinic_policy where facility_id=? and discharge_approval_required)", facility)
            && !db.exists("select exists(select 1 from medora.discharge_request where encounter_id=? and status='APPROVED')", encounter)) {
            throw conflict("DISCHARGE_APPROVAL_REQUIRED");
        }
    }
    private Map<String, Object> access(UUID facility, UUID id, String permission, boolean lock) {
        var actor = guard.require(facility, permission);
        var encounter = repository.encounter(facility, id, lock);
        if (!actor.getId().equals(encounter.get("doctorId")) && !actor.getId().equals(encounter.get("nurseId"))) {
            throw new AccessDeniedException("ENCOUNTER_NOT_ASSIGNED");
        }
        return encounter;
    }
    private void requireDraft(Map<String, Object> note) {
        if (!Set.of("DRAFT", "AMENDMENT_DRAFT").contains(note.get("status"))) { throw conflict("SIGNED_NOTE_IMMUTABLE"); }
    }
    private void open(Map<String, Object> encounter) {
        if (encounter.get("status").equals("COMPLETED")) { throw conflict("ENCOUNTER_COMPLETED"); }
    }
    private void requireState(Map<String, Object> encounter, String state) {
        if (!encounter.get("status").equals(state)) { throw conflict("INVALID_ENCOUNTER_STATE"); }
    }
    private void reason(String value) { if (optional(value) == null) { throw bad("CORRECTION_REASON_REQUIRED"); } }
    private void record(UUID facility, UUID id, String action) {
        audit.record(guard.currentActor().getId(), facility, action, "ENCOUNTER", id);
    }
    private void recordEntry(UUID facility, UUID encounter, UUID note, String action) {
        audit.record(guard.currentActor().getId(), facility, action, "ENCOUNTER", encounter, note, null);
    }
}
