package vn.medora.api.patient;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import vn.medora.api.identity.*;
import vn.medora.api.common.*;
import java.util.*;
import static vn.medora.api.common.ApiSupport.*;
import static vn.medora.api.patient.PatientDtos.*;
import static vn.medora.api.identity.AccountDtos.PageView;

@Service @Transactional
public class PatientService {
    private final PatientRepository patients;
    private final FacilityRepository facilities;
    private final PermissionGuard guard;
    private final AuditService audit;
    public PatientService(PatientRepository patients, FacilityRepository facilities,
        PermissionGuard guard, AuditService audit) {
        this.patients = patients; this.facilities = facilities;
        this.guard = guard; this.audit = audit;
    }
    public PageView<PatientView> search(UUID facility, String q, int page, int size) {
        var actor = guard.require(facility, "patient.admin.read");
        if (q.length() > 160) { throw bad("SEARCH_TOO_LONG"); }
        var result = patients.search(facility, like(q), page(page, size, "code"));
        audit.record(actor.getId(), facility, "PATIENT_SEARCH", "FACILITY", facility);
        return new PageView<>(result.getContent().stream().map(PatientView::of).toList(),
            page, size, result.getTotalElements());
    }
    public PatientView get(UUID facility, UUID id) {
        var actor = guard.require(facility, "patient.admin.read");
        var patient = patients.findByIdAndFacilityId(id, facility).orElseThrow(ApiSupport::missing);
        audit.record(actor.getId(), facility, "PATIENT_READ", "PATIENT", id);
        return PatientView.of(patient);
    }
    public List<PatientView> duplicates(UUID facility, PatientInput request) {
        var actor = guard.require(facility, "patient.admin.create");
        audit.record(actor.getId(), facility, "PATIENT_DUPLICATE_REVIEW", "FACILITY", facility);
        return candidates(facility, request).stream().map(PatientView::of).toList();
    }
    public PatientView create(UUID facility, PatientInput request) {
        var actor = guard.require(facility, "patient.admin.create");
        facilities.lock(facility).orElseThrow(ApiSupport::missing);
        boolean duplicate = reviewDuplicates(facility, null, request);
        var patient = patients.saveAndFlush(new Patient(facility, request));
        audit.record(actor.getId(), facility, "PATIENT_CREATED", "PATIENT", patient.getId());
        if (duplicate) { recordReview(actor.getId(), facility, patient.getId(), request); }
        return PatientView.of(patient);
    }
    public PatientView update(UUID facility, UUID id, PatientInput request) {
        var actor = guard.require(facility, "patient.admin.update");
        facilities.lock(facility).orElseThrow(ApiSupport::missing);
        var patient = patients.lock(id, facility).orElseThrow(ApiSupport::missing);
        if (patient.getMergedIntoId() != null) { throw conflict("PATIENT_MERGED_USE_CANONICAL_RECORD"); }
        version(patient, request.version());
        boolean duplicate = reviewDuplicates(facility, id, request);
        patient.update(request);
        patients.flush();
        audit.record(actor.getId(), facility, "PATIENT_UPDATED", "PATIENT", id);
        if (duplicate) { recordReview(actor.getId(), facility, id, request); }
        return PatientView.of(patient);
    }
    private List<Patient> candidates(UUID facility, PatientInput request) {
        return patients.duplicates(facility, normalized(request.fullName()),
            request.dateOfBirth(), optional(request.phone()), optional(request.nationalId()),
            PageRequest.of(0, 100));
    }
    private boolean reviewDuplicates(UUID facility, UUID exclude, PatientInput request) {
        if (candidates(facility, request).stream().anyMatch(p -> !p.getId().equals(exclude))) {
            if (!Boolean.TRUE.equals(request.duplicateReviewed()) || optional(request.duplicateReviewReason()) == null) {
                throw conflict("POSSIBLE_DUPLICATE_REQUIRES_REVIEW");
            }
            return true;
        }
        return false;
    }
    private void recordReview(UUID actor, UUID facility, UUID patient, PatientInput request) {
        audit.record(actor, facility, "DUPLICATE_ACKNOWLEDGED", "PATIENT", patient, null,
            request.duplicateReviewReason().strip());
    }
}
