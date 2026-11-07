package vn.medora.api.governance;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import static vn.medora.api.governance.GovernanceDtos.*;

@RestController @RequestMapping("/api/facilities/{facility}")
public class NotificationController {
    private final NotificationService service;
    public NotificationController(NotificationService service) { this.service=service; }
    @GetMapping("/notification-templates") public List<Map<String,Object>> templates(@PathVariable UUID facility) { return service.templates(facility); }
    @PostMapping("/notification-templates") public Map<String,Object> template(@PathVariable UUID facility,@Valid @RequestBody TemplateInput r) { return service.template(facility,null,r); }
    @PutMapping("/notification-templates/{id}") public Map<String,Object> template(@PathVariable UUID facility,@PathVariable UUID id,@Valid @RequestBody TemplateInput r) { return service.template(facility,id,r); }
    @PostMapping("/notifications") public Map<String,Object> queue(@PathVariable UUID facility,@RequestHeader(value="Idempotency-Key",required=false) String key,@Valid @RequestBody NotifyInput r) { return service.queue(facility,key,r); }
    @GetMapping("/notifications") public List<Map<String,Object>> status(@PathVariable UUID facility,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return service.status(facility,page,size); }
    @GetMapping("/inbox") public List<Map<String,Object>> inbox(@PathVariable UUID facility,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return service.inbox(facility,page,size); }
    @PostMapping("/inbox/{id}/read") public Map<String,Object> read(@PathVariable UUID facility,@PathVariable UUID id) { return service.read(facility,id); }
}
