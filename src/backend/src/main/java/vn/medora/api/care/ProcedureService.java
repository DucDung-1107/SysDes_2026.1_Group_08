package vn.medora.api.care;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.medora.api.common.*;
import java.time.Instant;
import java.util.*;
import static vn.medora.api.common.ApiSupport.*;
import static vn.medora.api.common.SqlRows.*;
import static vn.medora.api.common.ClinicAccess.*;
import static vn.medora.api.scheduling.SchedulingService.expected;
import static vn.medora.api.care.CareDtos.*;

@Service @Transactional
public class ProcedureService {
    private final Store db; private final ClinicAccess access; private final AuditService audit;
    public ProcedureService(Store db,ClinicAccess access,AuditService audit) { this.db=db; this.access=access; this.audit=audit; }
    private Map<String,Object> order(UUID facility,UUID id) { return db.one("select * from medora.procedure_order where id=? and facility_id=?",id,facility); }
    private Map<String,Object> doctor(UUID facility,Map<String,Object> row) {
        var visit=access.doctor(facility,uuid(row,"encounterId"),true); access.open(visit);
        state(visit,"IN_CONSULTATION","AWAITING_SERVICE"); return visit;
    }
    public Map<String,Object> requirements(UUID facility,UUID id,RequirementsInput r) {
        UUID actor=access.require(facility,"catalog.manage"); access.lock(facility);
        var service=db.one("select * from medora.clinic_service where id=? and facility_id=?",id,facility); expected(service,r.version());
        db.update("update medora.clinic_service set required_room_type=?,required_equipment=?,version=version+1 where id=?",
            optional(r.requiredRoomType()),r.requiredEquipment(),id);
        audit.record(actor,facility,"SERVICE_REQUIREMENTS_SAVED","SERVICE",id);
        return db.one("select * from medora.clinic_service where id=?",id);
    }
    public Map<String,Object> consultation(UUID facility,UUID encounter,ServiceSelection r) {
        access.require(facility,"clinical.write"); access.lock(facility); var visit=access.doctor(facility,encounter,true); access.open(visit);
        db.one("select id from medora.clinic_service where id=? and facility_id=? and enabled",r.serviceId(),facility);
        if(visit.get("appointmentId")!=null) { throw conflict("CONSULTATION_SERVICE_FIXED_BY_APPOINTMENT"); }
        db.update("update medora.encounter set consultation_service_id=?,version=version+1 where id=?",r.serviceId(),encounter);
        audit.record(access.actor(),facility,"CONSULTATION_SERVICE_SELECTED","ENCOUNTER",encounter);
        return db.one("select * from medora.encounter where id=?",encounter);
    }
    public Map<String,Object> create(UUID facility,UUID encounter,OrderInput r) {
        access.require(facility,"clinical.write"); access.lock(facility); var visit=access.doctor(facility,encounter,true); access.open(visit);
        state(visit,"IN_CONSULTATION","AWAITING_SERVICE");
        var service=db.one("select * from medora.clinic_service where id=? and facility_id=? and enabled",r.serviceId(),facility);
        UUID id=UUID.randomUUID(); db.update("""
            insert into medora.procedure_order(id,facility_id,encounter_id,service_id,service_name,reason,priority,preparation,required,kind)
            values(?,?,?,?,?,?,?,?,?,?)
            """,id,facility,encounter,r.serviceId(),service.get("name"),r.reason(),r.priority(),r.preparation(),r.required(),r.kind());
        db.update("update medora.encounter set status='AWAITING_SERVICE',version=version+1 where id=?",encounter);
        event(facility,id,"ORDER_CREATED",r.reason()); return order(facility,id);
    }
    public List<Map<String,Object>> list(UUID facility,UUID encounter) {
        access.encounter(facility,encounter,"clinical.read",false);
        audit.record(access.actor(),facility,"PROCEDURE_ORDERS_READ","ENCOUNTER",encounter);
        return db.list("select * from medora.procedure_order where facility_id=? and encounter_id=? order by created_at,id limit 100",facility,encounter);
    }
    public List<Map<String,Object>> queue(UUID facility,int page,int size) {
        UUID actor=access.require(facility,"procedure.execute"); page(page,size,"id");
        return db.list("select * from medora.procedure_order where facility_id=? and staff_id=? order by starts_at,id limit ? offset ?",facility,actor,size,(long)page*size);
    }
    public Map<String,Object> get(UUID facility,UUID id) {
        var row=order(facility,id);
        if(access.permits(facility,"procedure.execute") && access.actor().equals(row.get("staffId"))) { }
        else { access.encounter(facility,uuid(row,"encounterId"),"clinical.read",false); }
        audit.record(access.actor(),facility,"PROCEDURE_ORDER_READ","PROCEDURE_ORDER",id);
        var result=new LinkedHashMap<>(row); result.put("events",db.list("select * from medora.procedure_event where order_id=? order by occurred_at,id limit 100",id)); return result;
    }
    private Set<String> equipment(String text) {
        Set<String> result=new HashSet<>(); for(String part:text.split(",")) { if(!part.isBlank()) { result.add(part.strip().toUpperCase(Locale.ROOT)); } } return result;
    }
    public Map<String,Object> allocate(UUID facility,UUID id,AllocationInput r) {
        access.require(facility,"clinical.write"); access.lock(facility); var row=order(facility,id); doctor(facility,row);
        expected(row,r.version()); state(row,"CREATED","PENDING_ALLOCATION","SCHEDULED","DELAYED");
        access.staff(facility,r.staffId(),"TECHNICIAN");
        db.one("select id from medora.user_account where id=? for update",r.staffId());
        var service=db.one("select * from medora.clinic_service where id=? and facility_id=? and enabled",row.get("serviceId"),facility);
        var room=db.one("select * from medora.room where id=? and facility_id=? and enabled",r.roomId(),facility);
        if(service.get("requiredRoomType")!=null && !service.get("requiredRoomType").equals(room.get("roomType"))
            || !equipment((String)room.get("equipment")).containsAll(equipment((String)service.get("requiredEquipment")))) {
            throw conflict("ROOM_REQUIREMENTS_NOT_MET");
        }
        Instant end=r.startsAt().plusSeconds(number(service,"durationMinutes")*60);
        Instant until=end.plusSeconds(number(service,"bufferMinutes")*60);
        if(r.startsAt().isBefore(Instant.now().minusSeconds(300))) { throw bad("ALLOCATION_TOO_FAR_IN_PAST"); }
        boolean available=db.exists("""
            select exists(select 1 from medora.staff_schedule where facility_id=? and provider_id=? and kind='WORK' and starts_at<=? and ends_at>=?)
            and exists(select 1 from medora.provider_service where facility_id=? and provider_id=? and service_id=?)
            and not exists(select 1 from medora.staff_schedule where provider_id=? and kind='ABSENCE' and starts_at<? and ends_at>?)
            and not exists(select 1 from medora.resource_booking where active and (resource_id=? or resource_id=?) and starts_at<? and ends_at>?
                and (procedure_order_id is null or procedure_order_id<>?))
            """,facility,r.staffId(),r.startsAt(),until,facility,r.staffId(),row.get("serviceId"),r.staffId(),until,r.startsAt(),
            r.staffId(),r.roomId(),until,r.startsAt(),id);
        if(!available) { throw conflict("SLOT_UNAVAILABLE"); }
        for(var resource:Map.of("PROVIDER",r.staffId(),"ROOM",r.roomId()).entrySet()) {
            db.update("""
                insert into medora.resource_booking(id,procedure_order_id,kind,resource_id,starts_at,ends_at) values(?,?,?,?,?,?)
                on conflict(procedure_order_id,kind) do update set resource_id=excluded.resource_id,starts_at=excluded.starts_at,ends_at=excluded.ends_at,active=true
                """,UUID.randomUUID(),id,resource.getKey(),resource.getValue(),r.startsAt(),until);
        }
        db.update("update medora.procedure_order set staff_id=?,room_id=?,starts_at=?,ends_at=?,reserved_until=?,status='SCHEDULED',version=version+1 where id=?",
            r.staffId(),r.roomId(),r.startsAt(),end,until,id);
        event(facility,id,"ALLOCATED",r.reason()); return order(facility,id);
    }
    public Map<String,Object> action(UUID facility,UUID id,OrderAction r) {
        access.require(facility,r.action().equals("CANCEL")?"clinical.write":"procedure.execute");
        access.lock(facility); var row=order(facility,id);
        var visit=db.one("select * from medora.encounter where id=? and facility_id=? for update",row.get("encounterId"),facility); access.open(visit);
        expected(row,r.version());
        boolean clinician=r.action().equals("CANCEL");
        if(clinician) { doctor(facility,row); }
        else {
            UUID actor=access.require(facility,"procedure.execute");
            if(!actor.equals(row.get("staffId"))) { throw new AccessDeniedException("PROCEDURE_NOT_ASSIGNED"); }
        }
        switch(r.action()) {
            case "START" -> {
                state(row,"SCHEDULED");
                if(instant(row,"startsAt").isAfter(Instant.now()) || !instant(row,"endsAt").isAfter(Instant.now())) { throw conflict("OUTSIDE_ALLOCATED_SLOT"); }
                db.update("update medora.procedure_order set status='IN_PROGRESS',started_at=current_timestamp,version=version+1 where id=?",id);
            }
            case "COMPLETE" -> {
                state(row,"IN_PROGRESS"); if(optional(r.result())==null) { throw bad("SERVICE_RESULT_REQUIRED"); }
                db.update("update medora.procedure_order set status='COMPLETED',result=?,completed_at=current_timestamp,completed_by=?,version=version+1 where id=?",r.result(),access.actor(),id);
                release(id);
            }
            case "DELAY" -> {
                state(row,"SCHEDULED"); reason(r.reason());
                db.update("update medora.procedure_order set status='DELAYED',resolution_reason=?,version=version+1 where id=?",r.reason(),id); release(id);
            }
            case "CANCEL","NOT_PERFORMED" -> {
                state(row,"CREATED","PENDING_ALLOCATION","SCHEDULED","DELAYED","IN_PROGRESS"); reason(r.reason());
                db.update("update medora.procedure_order set status=?,resolution_reason=?,version=version+1 where id=?",
                    r.action().equals("CANCEL")?"CANCELED":"NOT_PERFORMED",r.reason(),id); release(id);
            }
            default -> throw bad("INVALID_ACTION");
        }
        event(facility,id,r.action(),r.reason()); return order(facility,id);
    }
    public Map<String,Object> review(UUID facility,UUID id,ReasonVersion r) {
        access.require(facility,"clinical.write"); access.lock(facility); var row=order(facility,id); doctor(facility,row);
        expected(row,r.version()); state(row,"COMPLETED","CANCELED","NOT_PERFORMED"); reason(r.reason());
        db.update("update medora.procedure_order set reviewed_by=?,reviewed_at=current_timestamp,version=version+1 where id=?",access.actor(),id);
        UUID encounter=uuid(row,"encounterId");
        if(!db.exists("select exists(select 1 from medora.procedure_order where encounter_id=? and (status not in ('COMPLETED','CANCELED','NOT_PERFORMED') or reviewed_at is null))",encounter)) {
            db.update("update medora.encounter set status='IN_CONSULTATION',version=version+1 where id=? and status='AWAITING_SERVICE'",encounter);
        }
        event(facility,id,"RESULT_REVIEWED",r.reason()); return order(facility,id);
    }
    private void release(UUID id) { db.update("update medora.resource_booking set active=false where procedure_order_id=?",id); }
    private void event(UUID facility,UUID id,String action,String reason) {
        db.update("insert into medora.procedure_event(id,order_id,actor_id,action,reason) values(?,?,?,?,?)",UUID.randomUUID(),id,access.actor(),action,reason);
        audit.record(access.actor(),facility,"PROCEDURE_"+action,"PROCEDURE_ORDER",id);
    }
}
