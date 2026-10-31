package vn.medora.api.patient;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vn.medora.api.identity.AccountDtos.PageView;
import java.util.*;
import static vn.medora.api.patient.PatientDtos.*;

@RestController @RequestMapping("/api/facilities/{facility}/patients")
public class PatientController {
    private final PatientService service;
    public PatientController(PatientService service) { this.service = service; }
    @GetMapping public PageView<PatientView> search(@PathVariable UUID facility,
        @RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) { return service.search(facility, q, page, size); }
    @GetMapping("/{id}") public PatientView get(@PathVariable UUID facility,
        @PathVariable UUID id) { return service.get(facility, id); }
    @PostMapping("/duplicate-check") public List<PatientView> duplicates(@PathVariable UUID facility,
        @Valid @RequestBody PatientInput request) { return service.duplicates(facility, request); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public PatientView create(@PathVariable UUID facility, @Valid @RequestBody PatientInput request) {
        return service.create(facility, request);
    }
    @PutMapping("/{id}") public PatientView update(@PathVariable UUID facility,
        @PathVariable UUID id, @Valid @RequestBody PatientInput request) {
        return service.update(facility, id, request);
    }
}
