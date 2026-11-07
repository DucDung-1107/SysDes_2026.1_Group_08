package vn.medora.api.scheduling;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.*;
import static vn.medora.api.common.SqlRows.*;
import static vn.medora.api.common.ApiSupport.*;
import static vn.medora.api.scheduling.SchedulingDtos.*;

@Repository
public class SchedulingRepository {
    private final JdbcTemplate jdbc;
    public SchedulingRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    private Map<String, Object> one(String sql, Object... params) {
        return jdbc.query(sql, MAPPER, params).stream().findFirst().orElseThrow(() -> missing());
    }
    public List<Map<String, Object>> rooms(UUID facility) {
        return jdbc.query("select * from medora.room where facility_id=? order by code limit 100", MAPPER, facility);
    }
    public List<Map<String, Object>> services(UUID facility) {
        return jdbc.query("select * from medora.clinic_service where facility_id=? order by code limit 100", MAPPER, facility);
    }
    public Map<String, Object> room(UUID facility, UUID id) {
        return one("select * from medora.room where facility_id=? and id=?", facility, id);
    }
    public Map<String, Object> service(UUID facility, UUID id) {
        return one("select * from medora.clinic_service where facility_id=? and id=?", facility, id);
    }
    public Map<String, Object> saveRoom(UUID facility, UUID id, RoomInput r) {
        if (id == null) {
            id = UUID.randomUUID();
            jdbc.update("""
                insert into medora.room(id,facility_id,code,name,room_type,equipment,enabled)
                values(?,?,?,?,?,?,?)
                """, id, facility, r.code(), r.name().strip(), r.roomType(), r.equipment(), r.enabled());
        } else if (jdbc.update("""
                update medora.room set code=?,name=?,room_type=?,equipment=?,enabled=?,version=version+1
                where id=? and facility_id=? and version=?
                """, r.code(), r.name().strip(), r.roomType(), r.equipment(), r.enabled(), id, facility, r.version()) != 1) {
            throw conflict("STALE_VERSION");
        }
        return room(facility, id);
    }
    public Map<String, Object> saveService(UUID facility, UUID id, ServiceInput r) {
        if (id == null) {
            id = UUID.randomUUID();
            jdbc.update("""
                insert into medora.clinic_service(id,facility_id,code,name,duration_minutes,buffer_minutes,enabled)
                values(?,?,?,?,?,?,?)
                """, id, facility, r.code(), r.name().strip(), r.durationMinutes(), r.bufferMinutes(), r.enabled());
        } else if (jdbc.update("""
                update medora.clinic_service set code=?,name=?,duration_minutes=?,buffer_minutes=?,enabled=?,version=version+1
                where id=? and facility_id=? and version=?
                """, r.code(), r.name().strip(), r.durationMinutes(), r.bufferMinutes(), r.enabled(), id, facility, r.version()) != 1) {
            throw conflict("STALE_VERSION");
        }
        return service(facility, id);
    }
    public void capability(UUID facility, CapabilityInput r) {
        service(facility, r.serviceId());
        jdbc.update("""
            insert into medora.provider_service(facility_id,provider_id,service_id) values(?,?,?) on conflict do nothing
            """, facility, r.providerId(), r.serviceId());
    }
    public Map<String, Object> addSchedule(UUID facility, ScheduleInput r) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
            insert into medora.staff_schedule(id,facility_id,provider_id,kind,starts_at,ends_at) values(?,?,?,?,?,?)
            """, id, facility, r.providerId(), r.kind(), time(r.startsAt()), time(r.endsAt()));
        return one("select * from medora.staff_schedule where id=?", id);
    }
    public boolean hasActiveAppointments(UUID provider, Instant start, Instant end) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
            select exists(select 1 from medora.resource_booking where resource_id=? and active
            and starts_at < ? and ends_at > ?)
            """, Boolean.class, provider, time(end), time(start)));
    }
    public List<Map<String, Object>> schedules(UUID facility, Instant from, Instant to) {
        return jdbc.query("""
            select s.*, u.display_name as provider_name from medora.staff_schedule s
            join medora.user_account u on u.id=s.provider_id
            where s.facility_id=? and s.starts_at < ? and s.ends_at > ? order by s.starts_at,s.id limit 100
            """, MAPPER, facility, time(to), time(from));
    }
    public boolean available(UUID facility, UUID provider, UUID room, UUID service,
        Instant start, Instant end, UUID exclude) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
            select exists(select 1 from medora.staff_schedule s
                where s.facility_id=? and s.provider_id=? and s.kind='WORK' and s.starts_at<=? and s.ends_at>=?)
            and exists(select 1 from medora.provider_service where facility_id=? and provider_id=? and service_id=?)
            and not exists(select 1 from medora.staff_schedule where provider_id=? and kind='ABSENCE'
                and starts_at < ? and ends_at > ?)
            and not exists(select 1 from medora.resource_booking where active and (resource_id=? or resource_id=?)
                and starts_at < ? and ends_at > ? and appointment_id<>?)
            """, Boolean.class, facility, provider, time(start), time(end), facility, provider, service,
            provider, time(end), time(start), provider, room, time(end), time(start),
            exclude == null ? new UUID(0, 0) : exclude));
    }
    public Optional<Map<String, Object>> byKey(UUID facility, String key) {
        return jdbc.query("select * from medora.appointment where facility_id=? and request_key=?", MAPPER, facility, key)
            .stream().findFirst();
    }
    public Map<String, Object> appointment(UUID facility, UUID id) {
        return one("select * from medora.appointment where facility_id=? and id=?", facility, id);
    }
    public List<Map<String, Object>> appointments(UUID facility, Instant from, Instant to,
        UUID actor, boolean fullQueue, int page, int size) {
        return jdbc.query("""
            select a.id,a.facility_id,a.patient_id,a.provider_id,a.service_id,a.room_id,
                a.starts_at,a.ends_at,a.status,a.version,p.code as patient_code,p.full_name as patient_name
            from medora.appointment a join medora.patient p on p.id=a.patient_id
            where a.facility_id=? and a.starts_at>=? and a.starts_at<? and (? or a.provider_id=?)
            order by a.starts_at,a.id limit ? offset ?
            """, MAPPER, facility, time(from), time(to), fullQueue, actor, size, (long) page * size);
    }
    public Map<String, Object> book(UUID facility, BookingInput r, Instant end, Instant until,
        String key, String hash) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
            insert into medora.appointment(id,facility_id,patient_id,provider_id,service_id,room_id,
                starts_at,ends_at,reserved_until,status,request_key,request_hash)
            values(?,?,?,?,?,?,?,?,?,'BOOKED',?,?)
            """, id, facility, r.patientId(), r.providerId(), r.serviceId(), r.roomId(), time(r.startsAt()),
            time(end), time(until), key, hash);
        reserve(id, r.providerId(), r.roomId(), r.startsAt(), until);
        return appointment(facility, id);
    }
    public Map<String, Object> reschedule(UUID facility, UUID id, RescheduleInput r, Instant end, Instant until) {
        jdbc.update("""
            update medora.appointment set provider_id=?,service_id=?,room_id=?,starts_at=?,ends_at=?,
                reserved_until=?,status='BOOKED',version=version+1 where id=? and facility_id=?
            """, r.providerId(), r.serviceId(), r.roomId(), time(r.startsAt()), time(end), time(until), id, facility);
        reserve(id, r.providerId(), r.roomId(), r.startsAt(), until);
        return appointment(facility, id);
    }
    private void reserve(UUID appointment, UUID provider, UUID room, Instant start, Instant end) {
        for (var entry : Map.of("PROVIDER", provider, "ROOM", room).entrySet()) {
            jdbc.update("""
                insert into medora.resource_booking(id,appointment_id,kind,resource_id,starts_at,ends_at)
                values(?,?,?,?,?,?) on conflict(appointment_id,kind) do update set
                resource_id=excluded.resource_id,starts_at=excluded.starts_at,ends_at=excluded.ends_at,active=true
                """, UUID.randomUUID(), appointment, entry.getKey(), entry.getValue(), time(start), time(end));
        }
    }
    public Map<String, Object> transition(UUID facility, UUID id, String status) {
        jdbc.update("update medora.appointment set status=?,version=version+1 where id=? and facility_id=?", status, id, facility);
        if (Set.of("CANCELED", "NO_SHOW").contains(status)) {
            jdbc.update("update medora.resource_booking set active=false where appointment_id=?", id);
        }
        return appointment(facility, id);
    }
    public void event(UUID facility, UUID appointment, UUID actor, String action, String reason) {
        jdbc.update("""
            insert into medora.appointment_event(id,facility_id,appointment_id,actor_id,action,reason) values(?,?,?,?,?,?)
            """, UUID.randomUUID(), facility, appointment, actor, action, reason);
    }
    public List<Map<String, Object>> events(UUID facility, UUID id) {
        return jdbc.query("select * from medora.appointment_event where facility_id=? and appointment_id=? order by occurred_at,id limit 100",
            MAPPER, facility, id);
    }
}
