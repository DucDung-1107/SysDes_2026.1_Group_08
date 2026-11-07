package vn.medora.api.scheduling;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.medora.api.common.*;
import vn.medora.api.identity.*;
import vn.medora.api.patient.PatientRepository;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import static vn.medora.api.common.ApiSupport.*;
import static vn.medora.api.common.SqlRows.*;
import static vn.medora.api.scheduling.SchedulingDtos.*;

@Service @Transactional
public class SchedulingService {
    private final SchedulingRepository repository;
    private final PermissionGuard guard;
    private final FacilityRepository facilities;
    private final UserAccountRepository accounts;
    private final UserRoleAssignmentRepository assignments;
    private final PatientRepository patients;
    private final AuditService audit;
    private final Store db;
    public SchedulingService(SchedulingRepository repository, PermissionGuard guard,
        FacilityRepository facilities, UserAccountRepository accounts,
        UserRoleAssignmentRepository assignments, PatientRepository patients, AuditService audit, Store db) {
        this.repository = repository; this.guard = guard; this.facilities = facilities;
        this.accounts = accounts; this.assignments = assignments; this.patients = patients; this.audit = audit; this.db = db;
    }
    public List<Map<String, Object>> catalog(UUID facility, String type) {
        guard.require(facility, "schedule.read");
        return type.equals("rooms") ? repository.rooms(facility) : repository.services(facility);
    }
    public Map<String, Object> room(UUID facility, UUID id, RoomInput request) {
        var actor = guard.require(facility, "schedule.manage"); lockClinic(facility);
        if (id != null) { repository.room(facility, id); }
        var result = repository.saveRoom(facility, id, request);
        audit.record(actor.getId(), facility, "ROOM_SAVED", "ROOM", uuid(result, "id"));
        return result;
    }
    public Map<String, Object> service(UUID facility, UUID id, ServiceInput request) {
        var actor = guard.require(facility, "schedule.manage"); lockClinic(facility);
        if (id != null) { repository.service(facility, id); }
        var result = repository.saveService(facility, id, request);
        audit.record(actor.getId(), facility, "SERVICE_SAVED", "SERVICE", uuid(result, "id"));
        return result;
    }
    public void capability(UUID facility, CapabilityInput request) {
        var actor = guard.require(facility, "schedule.manage"); lockClinic(facility);
        requireServiceStaff(facility, request.providerId());
        repository.capability(facility, request);
        audit.record(actor.getId(), facility, "PROVIDER_CAPABILITY_ADDED", "USER", request.providerId());
    }
    public Map<String, Object> schedule(UUID facility, ScheduleInput request) {
        var actor = guard.require(facility, "schedule.manage"); lockClinic(facility);
        requireServiceStaff(facility, request.providerId());
        range(request.startsAt(), request.endsAt(), 31);
        accounts.findForUpdate(request.providerId()).orElseThrow(ApiSupport::missing);
        if (request.kind().equals("ABSENCE") && repository.hasActiveAppointments(
            request.providerId(), request.startsAt(), request.endsAt())) {
            throw conflict("RESCHEDULE_APPOINTMENTS_BEFORE_ABSENCE");
        }
        var result = repository.addSchedule(facility, request);
        audit.record(actor.getId(), facility, "STAFF_SCHEDULE_ADDED", "SCHEDULE", uuid(result, "id"));
        return result;
    }
    public List<Map<String, Object>> schedules(UUID facility, Instant from, Instant to) {
        guard.require(facility, "schedule.read"); range(from, to, 31);
        return repository.schedules(facility, from, to);
    }
    public List<Map<String, Object>> availability(UUID facility, UUID provider, UUID room,
        UUID service, Instant from, Instant to) {
        guard.require(facility, "schedule.read"); range(from, to, 1);
        requireStaff(facility, provider, "DOCTOR");
        var resource = enabled(repository.room(facility, room));
        var definition = enabled(repository.service(facility, service));
        long duration = number(definition, "durationMinutes");
        long buffer = number(definition, "bufferMinutes");
        List<Map<String, Object>> result = new ArrayList<>();
        for (Instant start = from; !start.plusSeconds((duration + buffer) * 60).isAfter(to)
            && result.size() < 100; start = start.plusSeconds(300)) {
            Instant end = start.plusSeconds((duration + buffer) * 60);
            if (start.isAfter(Instant.now()) && repository.available(facility, provider, room, service, start, end, null)) {
                result.add(Map.of("startsAt", start, "endsAt", start.plusSeconds(duration * 60),
                    "reservedUntil", end, "roomId", resource.get("id"), "providerId", provider));
            }
        }
        return result;
    }
    public List<Map<String, Object>> list(UUID facility, Instant from, Instant to, int page, int size) {
        var actor = guard.require(facility, "schedule.read"); range(from, to, 31); page(page, size, "id");
        boolean reception = guard.permissions(facility).contains("appointment.write");
        return repository.appointments(facility, from, to, actor.getId(), reception, page, size);
    }
    public Map<String, Object> book(UUID facility, String key, BookingInput request) {
        var actor = guard.require(facility, "appointment.write"); lockClinic(facility);
        if (key == null || !key.matches("[A-Za-z0-9._-]{8,100}")) { throw bad("IDEMPOTENCY_KEY_REQUIRED"); }
        String hash = fingerprint(request);
        var previous = repository.byKey(facility, key);
        if (previous.isPresent()) {
            if (!hash.equals(previous.get().get("requestHash"))) { throw conflict("IDEMPOTENCY_KEY_REUSED"); }
            return publicAppointment(previous.get());
        }
        patients.findByIdAndFacilityId(request.patientId(), facility).orElseThrow(ApiSupport::missing);
        if (db.exists("select exists(select 1 from medora.patient where id=? and merged_into_id is not null)", request.patientId())) {
            throw conflict("PATIENT_MERGED_USE_CANONICAL_RECORD");
        }
        Instant[] times = validateBooking(facility, request.providerId(), request.roomId(),
            request.serviceId(), request.startsAt(), null);
        var result = repository.book(facility, request, times[0], times[1], key, hash);
        record(actor.getId(), facility, uuid(result, "id"), "BOOKED", null);
        return publicAppointment(result);
    }
    public Map<String, Object> reschedule(UUID facility, UUID id, RescheduleInput request) {
        var actor = guard.require(facility, "appointment.write"); lockClinic(facility);
        var previous = repository.appointment(facility, id); expected(previous, request.version());
        requireBookable(previous);
        Instant[] times = validateBooking(facility, request.providerId(), request.roomId(),
            request.serviceId(), request.startsAt(), id);
        var result = repository.reschedule(facility, id, request, times[0], times[1]);
        record(actor.getId(), facility, id, "RESCHEDULED", request.reason());
        return publicAppointment(result);
    }
    public Map<String, Object> transition(UUID facility, UUID id, TransitionInput request) {
        var actor = guard.require(facility, "appointment.write"); lockClinic(facility);
        var previous = repository.appointment(facility, id); expected(previous, request.version());
        requireBookable(previous);
        String status = switch (request.action()) {
            case "CONFIRM" -> {
                if (!previous.get("status").equals("BOOKED")) { throw conflict("INVALID_APPOINTMENT_STATE"); }
                yield "CONFIRMED";
            }
            case "CANCEL" -> {
                if (optional(request.reason()) == null) { throw bad("CANCELLATION_REASON_REQUIRED"); }
                yield "CANCELED";
            }
            case "NO_SHOW" -> {
                if (instant(previous, "endsAt").isAfter(Instant.now())) { throw conflict("NO_SHOW_TOO_EARLY"); }
                yield "NO_SHOW";
            }
            default -> throw bad("INVALID_ACTION");
        };
        var result = repository.transition(facility, id, status);
        record(actor.getId(), facility, id, status, request.reason());
        return publicAppointment(result);
    }
    public List<Map<String, Object>> events(UUID facility, UUID id) {
        guard.require(facility, "appointment.write"); repository.appointment(facility, id);
        return repository.events(facility, id);
    }
    public void requireStaff(UUID facility, UUID account, String role) {
        var staff = accounts.findById(account).orElseThrow(ApiSupport::missing);
        if (staff.getStatus() != AccountStatus.ACTIVE || assignments.findByAccount_IdAndFacility_Id(account, facility)
            .stream().noneMatch(a -> a.getRole().getCode().equals(role) && a.getFacility().isEnabled())) {
            throw bad("STAFF_NOT_AVAILABLE_IN_FACILITY");
        }
    }
    private void requireServiceStaff(UUID facility, UUID account) {
        var staff = accounts.findById(account).orElseThrow(ApiSupport::missing);
        if (staff.getStatus() != AccountStatus.ACTIVE || assignments.findByAccount_IdAndFacility_Id(account, facility)
            .stream().noneMatch(a -> Set.of("DOCTOR", "TECHNICIAN").contains(a.getRole().getCode()) && a.getFacility().isEnabled())) {
            throw bad("STAFF_NOT_AVAILABLE_IN_FACILITY");
        }
    }
    public void lockClinic(UUID facility) { facilities.lock(facility).orElseThrow(ApiSupport::missing); }
    private Instant[] validateBooking(UUID facility, UUID provider, UUID room, UUID service, Instant start, UUID exclude) {
        requireStaff(facility, provider, "DOCTOR");
        accounts.findForUpdate(provider).orElseThrow(ApiSupport::missing);
        enabled(repository.room(facility, room));
        var definition = enabled(repository.service(facility, service));
        if (!start.isAfter(Instant.now())) { throw bad("APPOINTMENT_MUST_BE_IN_FUTURE"); }
        Instant end = start.plusSeconds(number(definition, "durationMinutes") * 60);
        Instant until = end.plusSeconds(number(definition, "bufferMinutes") * 60);
        if (!repository.available(facility, provider, room, service, start, until, exclude)) {
            throw conflict("SLOT_UNAVAILABLE");
        }
        return new Instant[]{end, until};
    }
    private Map<String, Object> enabled(Map<String, Object> resource) {
        if (!Boolean.TRUE.equals(resource.get("enabled"))) { throw conflict("RESOURCE_DISABLED"); }
        return resource;
    }
    private void record(UUID actor, UUID facility, UUID id, String action, String reason) {
        repository.event(facility, id, actor, action, reason);
        audit.record(actor, facility, "APPOINTMENT_" + action, "APPOINTMENT", id);
    }
    private void requireBookable(Map<String, Object> appointment) {
        if (!Set.of("BOOKED", "CONFIRMED").contains(appointment.get("status"))) {
            throw conflict("INVALID_APPOINTMENT_STATE");
        }
    }
    public static void expected(Map<String, Object> row, Long version) {
        if (version == null || number(row, "version") != version) { throw conflict("STALE_VERSION"); }
    }
    public static void range(Instant from, Instant to, int days) {
        if (!from.isBefore(to) || Duration.between(from, to).compareTo(Duration.ofDays(days)) > 0) {
            throw bad("INVALID_DATE_RANGE");
        }
    }
    private String fingerprint(BookingInput request) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(request.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }
    private Map<String, Object> publicAppointment(Map<String, Object> result) {
        var view = new LinkedHashMap<>(result); view.remove("requestHash"); view.remove("requestKey"); return view;
    }
}
