package vn.medora.api.care;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import static vn.medora.api.care.CareDtos.*;

@RestController @RequestMapping("/api/facilities/{facility}")
public class MedicationCatalogController {
    private final MedicationCatalogService service;
    public MedicationCatalogController(MedicationCatalogService service) { this.service=service; }
    @GetMapping("/medications") public List<Map<String,Object>> list(@PathVariable UUID facility,
        @RequestParam(defaultValue="") String q,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        return service.medicines(facility,q,page,size);
    }
    @PostMapping("/medications") public Map<String,Object> create(@PathVariable UUID facility,@Valid @RequestBody MedicationInput r) { return service.medicine(facility,null,r); }
    @PutMapping("/medications/{id}") public Map<String,Object> update(@PathVariable UUID facility,@PathVariable UUID id,@Valid @RequestBody MedicationInput r) { return service.medicine(facility,id,r); }
    @GetMapping("/medication-rules") public List<Map<String,Object>> rules(@PathVariable UUID facility) { return service.rules(facility); }
    @PostMapping("/medication-rules") public Map<String,Object> rule(@PathVariable UUID facility,@Valid @RequestBody RuleInput r) { return service.rule(facility,null,r); }
    @PutMapping("/medication-rules/{id}") public Map<String,Object> rule(@PathVariable UUID facility,@PathVariable UUID id,@Valid @RequestBody RuleInput r) { return service.rule(facility,id,r); }
    @GetMapping("/medication-policy") public Map<String,Object> policy(@PathVariable UUID facility) { return service.policy(facility); }
    @PutMapping("/medication-policy") public Map<String,Object> policy(@PathVariable UUID facility,@Valid @RequestBody SafetyPolicyInput r) { return service.policy(facility,r); }
    @PutMapping("/encounters/{encounter}/medication-profile") public Map<String,Object> profile(@PathVariable UUID facility,@PathVariable UUID encounter,@Valid @RequestBody ProfileInput r) { return service.profile(facility,encounter,r); }
    @GetMapping("/encounters/{encounter}/medication-profile") public Map<String,Object> profile(@PathVariable UUID facility,@PathVariable UUID encounter) { return service.profile(facility,encounter); }
}
