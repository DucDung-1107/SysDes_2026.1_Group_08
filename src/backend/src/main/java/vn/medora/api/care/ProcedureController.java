package vn.medora.api.care;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import static vn.medora.api.care.CareDtos.*;

@RestController @RequestMapping("/api/facilities/{facility}")
public class ProcedureController {
    private final ProcedureService service;
    public ProcedureController(ProcedureService service) { this.service=service; }
    @PutMapping("/services/{id}/requirements") public Map<String,Object> requirements(@PathVariable UUID facility,@PathVariable UUID id,@Valid @RequestBody RequirementsInput r) { return service.requirements(facility,id,r); }
    @PutMapping("/encounters/{encounter}/consultation-service") public Map<String,Object> consultation(@PathVariable UUID facility,@PathVariable UUID encounter,@Valid @RequestBody ServiceSelection r) { return service.consultation(facility,encounter,r); }
    @GetMapping("/encounters/{encounter}/orders") public List<Map<String,Object>> list(@PathVariable UUID facility,@PathVariable UUID encounter) { return service.list(facility,encounter); }
    @PostMapping("/encounters/{encounter}/orders") public Map<String,Object> create(@PathVariable UUID facility,@PathVariable UUID encounter,@Valid @RequestBody OrderInput r) { return service.create(facility,encounter,r); }
    @GetMapping("/procedure-queue") public List<Map<String,Object>> queue(@PathVariable UUID facility,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return service.queue(facility,page,size); }
    @GetMapping("/orders/{id}") public Map<String,Object> get(@PathVariable UUID facility,@PathVariable UUID id) { return service.get(facility,id); }
    @PutMapping("/orders/{id}/allocation") public Map<String,Object> allocate(@PathVariable UUID facility,@PathVariable UUID id,@Valid @RequestBody AllocationInput r) { return service.allocate(facility,id,r); }
    @PostMapping("/orders/{id}/actions") public Map<String,Object> action(@PathVariable UUID facility,@PathVariable UUID id,@Valid @RequestBody OrderAction r) { return service.action(facility,id,r); }
    @PostMapping("/orders/{id}/review") public Map<String,Object> review(@PathVariable UUID facility,@PathVariable UUID id,@Valid @RequestBody ReasonVersion r) { return service.review(facility,id,r); }
}
