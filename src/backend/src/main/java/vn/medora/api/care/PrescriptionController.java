package vn.medora.api.care;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import static vn.medora.api.care.CareDtos.*;

@RestController @RequestMapping("/api/facilities/{facility}")
public class PrescriptionController {
    private final PrescriptionService service;
    public PrescriptionController(PrescriptionService service) { this.service=service; }
    @GetMapping("/encounters/{encounter}/prescriptions") public List<Map<String,Object>> list(@PathVariable UUID facility,@PathVariable UUID encounter) { return service.list(facility,encounter); }
    @PostMapping("/encounters/{encounter}/prescriptions") public Map<String,Object> create(@PathVariable UUID facility,@PathVariable UUID encounter,@Valid @RequestBody RxInput r) { return service.create(facility,encounter,r); }
    @GetMapping("/pharmacy-queue") public List<Map<String,Object>> queue(@PathVariable UUID facility,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return service.pharmacy(facility,page,size); }
    @GetMapping("/prescriptions/{id}") public Map<String,Object> get(@PathVariable UUID facility,@PathVariable UUID id) { return service.get(facility,id,false); }
    @GetMapping("/prescriptions/{id}/export") public Map<String,Object> export(@PathVariable UUID facility,@PathVariable UUID id) { return service.get(facility,id,true); }
    @PutMapping("/prescriptions/{id}") public Map<String,Object> save(@PathVariable UUID facility,@PathVariable UUID id,@Valid @RequestBody RxInput r) { return service.save(facility,id,r); }
    @GetMapping("/prescriptions/{id}/safety") public Map<String,Object> safety(@PathVariable UUID facility,@PathVariable UUID id) { return service.safety(facility,id); }
    @PostMapping("/prescriptions/{id}/issue") public Map<String,Object> issue(@PathVariable UUID facility,@PathVariable UUID id,@RequestHeader(value="Idempotency-Key",required=false) String key,@Valid @RequestBody IssueInput r) { return service.issue(facility,id,key,r); }
    @PostMapping("/prescriptions/{id}/amendments") public Map<String,Object> amend(@PathVariable UUID facility,@PathVariable UUID id,@Valid @RequestBody ReasonVersion r) { return service.amend(facility,id,r); }
    @PostMapping("/prescriptions/{id}/cancel") public Map<String,Object> cancel(@PathVariable UUID facility,@PathVariable UUID id,@Valid @RequestBody ReasonVersion r) { return service.cancel(facility,id,r); }
    @PostMapping("/prescriptions/{id}/medicine-events") public Map<String,Object> event(@PathVariable UUID facility,@PathVariable UUID id,@RequestHeader(value="Idempotency-Key",required=false) String key,@Valid @RequestBody MedicineEventInput r) { return service.medicineEvent(facility,id,key,r); }
}
