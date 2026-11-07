package vn.medora.api.governance;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import vn.medora.api.finance.FinanceDtos.PolicyInput;
import java.time.Instant;
import java.util.*;
import static vn.medora.api.governance.GovernanceDtos.*;

@RestController @RequestMapping("/api/facilities/{facility}")
public class GovernanceController {
    private final GovernanceService service;
    public GovernanceController(GovernanceService service) { this.service=service; }
    @PutMapping("/diagnosis-catalog") public Map<String,Object> diagnosis(@PathVariable UUID facility,@Valid @RequestBody DiagnosisInput r) { return service.diagnosis(facility,r); }
    @GetMapping("/clinic-policy") public Map<String,Object> policy(@PathVariable UUID facility) { return service.policy(facility); }
    @PutMapping("/clinic-policy") public Map<String,Object> policy(@PathVariable UUID facility,@Valid @RequestBody PolicyInput r) { return service.policy(facility,r); }
    @PostMapping("/merge-requests") public Map<String,Object> merge(@PathVariable UUID facility,@RequestHeader(value="Idempotency-Key",required=false) String key,@Valid @RequestBody MergeInput r) { return service.mergeRequest(facility,key,r); }
    @GetMapping("/merge-requests") public List<Map<String,Object>> merges(@PathVariable UUID facility,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return service.merges(facility,page,size); }
    @PostMapping("/merge-requests/{id}/decision") public Map<String,Object> decide(@PathVariable UUID facility,@PathVariable UUID id,@RequestHeader(value="Idempotency-Key",required=false) String key,@Valid @RequestBody Decision r) { return service.decideMerge(facility,id,key,r); }
    @PostMapping("/encounters/{encounter}/discharge-requests") public Map<String,Object> discharge(@PathVariable UUID facility,@PathVariable UUID encounter,@Valid @RequestBody Reason r) { return service.discharge(facility,encounter,r); }
    @PostMapping("/discharge-requests/{id}/decision") public Map<String,Object> dischargeDecision(@PathVariable UUID facility,@PathVariable UUID id,@Valid @RequestBody DischargeDecision r) { return service.decideDischarge(facility,id,r); }
    @GetMapping("/audit-events") public List<Map<String,Object>> audits(@PathVariable UUID facility,@RequestParam Instant from,@RequestParam Instant to,
        @RequestParam(required=false) UUID actor,@RequestParam(required=false) UUID target,@RequestParam(required=false) String action,
        @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return service.audits(facility,from,to,actor,target,action,page,size); }
    @GetMapping("/patients/{patient}/audit-events") public List<Map<String,Object>> patientAudit(@PathVariable UUID facility,@PathVariable UUID patient,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return service.patientAudit(facility,patient,page,size); }
}
