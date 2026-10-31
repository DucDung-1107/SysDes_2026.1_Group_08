package vn.medora.api.scheduling;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.*;
import static vn.medora.api.scheduling.SchedulingDtos.*;

@RestController @RequestMapping("/api/facilities/{facility}")
public class SchedulingController {
    private final SchedulingService service;
    public SchedulingController(SchedulingService service) { this.service = service; }
    @GetMapping("/rooms") public List<Map<String, Object>> rooms(@PathVariable UUID facility) {
        return service.catalog(facility, "rooms");
    }
    @PostMapping("/rooms") @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> room(@PathVariable UUID facility, @Valid @RequestBody RoomInput r) {
        return service.room(facility, null, r);
    }
    @PutMapping("/rooms/{id}") public Map<String, Object> room(@PathVariable UUID facility,
        @PathVariable UUID id, @Valid @RequestBody RoomInput r) { return service.room(facility, id, r); }
    @GetMapping("/services") public List<Map<String, Object>> services(@PathVariable UUID facility) {
        return service.catalog(facility, "services");
    }
    @PostMapping("/services") @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> service(@PathVariable UUID facility, @Valid @RequestBody ServiceInput r) {
        return service.service(facility, null, r);
    }
    @PutMapping("/services/{id}") public Map<String, Object> service(@PathVariable UUID facility,
        @PathVariable UUID id, @Valid @RequestBody ServiceInput r) { return service.service(facility, id, r); }
    @PostMapping("/provider-services") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void capability(@PathVariable UUID facility, @Valid @RequestBody CapabilityInput r) {
        service.capability(facility, r);
    }
    @PostMapping("/staff-schedules") @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> schedule(@PathVariable UUID facility, @Valid @RequestBody ScheduleInput r) {
        return service.schedule(facility, r);
    }
    @GetMapping("/staff-schedules") public List<Map<String, Object>> schedules(@PathVariable UUID facility,
        @RequestParam Instant from, @RequestParam Instant to) { return service.schedules(facility, from, to); }
    @GetMapping("/availability") public List<Map<String, Object>> availability(@PathVariable UUID facility,
        @RequestParam UUID providerId, @RequestParam UUID roomId, @RequestParam UUID serviceId,
        @RequestParam Instant from, @RequestParam Instant to) {
        return service.availability(facility, providerId, roomId, serviceId, from, to);
    }
    @GetMapping("/appointments") public List<Map<String, Object>> list(@PathVariable UUID facility,
        @RequestParam Instant from, @RequestParam Instant to, @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) { return service.list(facility, from, to, page, size); }
    @PostMapping("/appointments") @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> book(@PathVariable UUID facility,
        @RequestHeader(value = "Idempotency-Key", required = false) String key,
        @Valid @RequestBody BookingInput r) { return service.book(facility, key, r); }
    @PutMapping("/appointments/{id}/schedule") public Map<String, Object> reschedule(@PathVariable UUID facility,
        @PathVariable UUID id, @Valid @RequestBody RescheduleInput r) { return service.reschedule(facility, id, r); }
    @PostMapping("/appointments/{id}/transitions") public Map<String, Object> transition(@PathVariable UUID facility,
        @PathVariable UUID id, @Valid @RequestBody TransitionInput r) { return service.transition(facility, id, r); }
    @GetMapping("/appointments/{id}/events") public List<Map<String, Object>> events(@PathVariable UUID facility,
        @PathVariable UUID id) { return service.events(facility, id); }
}
