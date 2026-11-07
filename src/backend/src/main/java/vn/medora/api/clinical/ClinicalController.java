package vn.medora.api.clinical;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vn.medora.api.common.VersionRequest;
import vn.medora.api.scheduling.SchedulingDtos.CheckInInput;
import java.util.*;
import static vn.medora.api.clinical.ClinicalDtos.*;

@RestController @RequestMapping("/api/facilities/{facility}")
public class ClinicalController {
    private final ClinicalService service;
    public ClinicalController(ClinicalService service) { this.service = service; }
    @PostMapping("/check-ins") @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> checkIn(@PathVariable UUID facility, @Valid @RequestBody CheckInInput r) {
        return service.checkIn(facility, r);
    }
    @GetMapping("/queue") public List<Map<String, Object>> queue(@PathVariable UUID facility,
        @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return service.queue(facility, page, size);
    }
    @GetMapping("/encounters/{id}") public Map<String, Object> get(@PathVariable UUID facility, @PathVariable UUID id) {
        return service.get(facility, id);
    }
    @PutMapping("/encounters/{id}/nurse") public Map<String, Object> nurse(@PathVariable UUID facility,
        @PathVariable UUID id, @Valid @RequestBody AssignmentInput r) { return service.assignNurse(facility, id, r); }
    @PostMapping("/encounters/{id}/start") public Map<String, Object> start(@PathVariable UUID facility,
        @PathVariable UUID id, @Valid @RequestBody VersionRequest r) { return service.start(facility, id, r.version()); }
    @GetMapping("/encounters/{id}/record") public List<Map<String, Object>> timeline(@PathVariable UUID facility,
        @PathVariable UUID id, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return service.timeline(facility, id, page, size);
    }
    @GetMapping("/encounters/{id}/record/{visit}") public Map<String, Object> past(@PathVariable UUID facility,
        @PathVariable UUID id, @PathVariable UUID visit) { return service.pastVisit(facility, id, visit); }
    @GetMapping("/encounters/{id}/history") public List<Map<String, Object>> history(@PathVariable UUID facility,
        @PathVariable UUID id, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return service.history(facility, id, page, size);
    }
    @PostMapping("/encounters/{id}/history") @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> history(@PathVariable UUID facility, @PathVariable UUID id,
        @Valid @RequestBody HistoryInput r) { return service.addHistory(facility, id, r); }
    @GetMapping("/encounters/{id}/vitals") public List<Map<String, Object>> vitals(@PathVariable UUID facility,
        @PathVariable UUID id, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return service.vitals(facility, id, page, size);
    }
    @PostMapping("/encounters/{id}/vitals") @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> vitals(@PathVariable UUID facility, @PathVariable UUID id,
        @Valid @RequestBody VitalInput r) { return service.addVitals(facility, id, r); }
    @GetMapping("/diagnosis-catalog") public List<Map<String, Object>> catalog(@PathVariable UUID facility) {
        return service.diagnosisCatalog(facility);
    }
    @GetMapping("/encounters/{id}/diagnoses") public List<Map<String, Object>> diagnoses(@PathVariable UUID facility,
        @PathVariable UUID id) { return service.diagnoses(facility, id); }
    @PostMapping("/encounters/{id}/diagnoses") @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> diagnosis(@PathVariable UUID facility, @PathVariable UUID id,
        @Valid @RequestBody DiagnosisInput r) { return service.addDiagnosis(facility, id, r); }
    @GetMapping("/encounters/{id}/notes") public List<Map<String, Object>> notes(@PathVariable UUID facility,
        @PathVariable UUID id, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return service.notes(facility, id, page, size);
    }
    @PostMapping("/encounters/{id}/notes") @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> draft(@PathVariable UUID facility, @PathVariable UUID id,
        @Valid @RequestBody NoteInput r) { return service.draft(facility, id, r); }
    @PutMapping("/encounters/{id}/notes/{note}") public Map<String, Object> save(@PathVariable UUID facility,
        @PathVariable UUID id, @PathVariable UUID note, @Valid @RequestBody NoteInput r) {
        return service.updateDraft(facility, id, note, r);
    }
    @PostMapping("/encounters/{id}/notes/{note}/sign") public Map<String, Object> sign(@PathVariable UUID facility,
        @PathVariable UUID id, @PathVariable UUID note, @Valid @RequestBody VersionRequest r) {
        return service.sign(facility, id, note, r.version());
    }
    @PostMapping("/encounters/{id}/notes/{note}/amendments") @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> amend(@PathVariable UUID facility, @PathVariable UUID id,
        @PathVariable UUID note, @Valid @RequestBody AmendmentInput r) { return service.amend(facility, id, note, r); }
    @PostMapping("/encounters/{id}/notes/{note}/abandon") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void abandon(@PathVariable UUID facility, @PathVariable UUID id,
        @PathVariable UUID note, @Valid @RequestBody VersionRequest r) { service.abandon(facility, id, note, r.version()); }
    @PostMapping("/encounters/{id}/submit-for-closure") public Map<String, Object> submit(@PathVariable UUID facility,
        @PathVariable UUID id, @Valid @RequestBody VersionRequest r) { return service.submit(facility, id, r.version()); }
    @PostMapping("/encounters/{id}/return-for-correction") public Map<String, Object> correct(@PathVariable UUID facility,
        @PathVariable UUID id, @Valid @RequestBody VersionRequest r) { return service.returnForCorrection(facility, id, r.version()); }
    @PostMapping("/encounters/{id}/complete") public Map<String, Object> complete(@PathVariable UUID facility,
        @PathVariable UUID id, @Valid @RequestBody CloseInput r) { return service.complete(facility, id, r); }
}
