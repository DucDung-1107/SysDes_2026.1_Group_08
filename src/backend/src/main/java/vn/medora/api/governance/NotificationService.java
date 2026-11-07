package vn.medora.api.governance;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.medora.api.common.*;
import java.util.*;
import static vn.medora.api.common.ApiSupport.*;
import static vn.medora.api.scheduling.SchedulingService.expected;
import static vn.medora.api.governance.GovernanceDtos.*;

@Service @Transactional
public class NotificationService {
    private final Store db; private final ClinicAccess access; private final AuditService audit; private final RequestDeduplication dedup;
    public NotificationService(Store db,ClinicAccess access,AuditService audit,RequestDeduplication dedup) { this.db=db; this.access=access; this.audit=audit; this.dedup=dedup; }
    public List<Map<String,Object>> templates(UUID facility) {
        if(!access.permits(facility,"notification.manage")) { access.require(facility,"notification.send"); }
        return db.list("select * from medora.notification_template where facility_id=? order by code limit 100",facility);
    }
    public Map<String,Object> template(UUID facility,UUID id,TemplateInput r) {
        UUID actor=access.require(facility,"notification.manage"); access.lock(facility);
        String remaining=r.body().replace("{{appointmentId}}","").replace("{{startsAt}}","");
        if(remaining.contains("{{")||remaining.contains("}}")) { throw bad("UNSUPPORTED_TEMPLATE_PLACEHOLDER"); }
        if(id==null) { id=UUID.randomUUID(); db.update("insert into medora.notification_template(id,facility_id,code,body,enabled) values(?,?,?,?,?)",id,facility,r.code(),r.body(),r.enabled()); }
        else {
            expected(db.one("select * from medora.notification_template where id=? and facility_id=?",id,facility),r.version());
            db.update("update medora.notification_template set code=?,body=?,enabled=?,version=version+1 where id=?",r.code(),r.body(),r.enabled(),id);
        }
        audit.record(actor,facility,"NOTIFICATION_TEMPLATE_SAVED","NOTIFICATION_TEMPLATE",id);
        return db.one("select * from medora.notification_template where id=?",id);
    }
    public Map<String,Object> queue(UUID facility,String key,NotifyInput r) {
        UUID actor=access.require(facility,"notification.send"); access.lock(facility);
        var previous=dedup.previous(facility,actor,"NOTIFICATION",key,r); if(previous.isPresent()) { return previous.get(); }
        var template=db.one("select * from medora.notification_template where id=? and facility_id=? and enabled",r.templateId(),facility);
        var appointment=db.one("select * from medora.appointment where id=? and facility_id=?",r.appointmentId(),facility);
        if(!db.exists("""
            select exists(select 1 from medora.user_role_assignment a join medora.user_account u on u.id=a.user_account_id
            where a.facility_id=? and a.user_account_id=? and u.status='ACTIVE')
            """,facility,r.recipientId())) { throw bad("RECIPIENT_NOT_IN_FACILITY"); }
        String body=template.get("body").toString().replace("{{appointmentId}}",r.appointmentId().toString()).replace("{{startsAt}}",appointment.get("startsAt").toString());
        if(body.length()>2000) { throw bad("NOTIFICATION_TOO_LONG"); }
        UUID id=UUID.randomUUID(); db.update("insert into medora.notification(id,facility_id,template_id,appointment_id,recipient_id,body) values(?,?,?,?,?,?)",
            id,facility,r.templateId(),r.appointmentId(),r.recipientId(),body);
        audit.record(actor,facility,"NOTIFICATION_QUEUED","NOTIFICATION",id);
        return dedup.save(facility,actor,"NOTIFICATION",key,r,db.one("select * from medora.notification where id=?",id));
    }
    private void member(UUID facility) {
        if(!db.exists("select exists(select 1 from medora.user_role_assignment where facility_id=? and user_account_id=?)",facility,access.actor())) {
            throw new org.springframework.security.access.AccessDeniedException("FORBIDDEN");
        }
    }
    public List<Map<String,Object>> inbox(UUID facility,int page,int size) {
        member(facility); page(page,size,"id");
        return db.list("select * from medora.notification where facility_id=? and recipient_id=? and status='DELIVERED' order by created_at desc,id limit ? offset ?",facility,access.actor(),size,(long)page*size);
    }
    public Map<String,Object> read(UUID facility,UUID id) {
        member(facility); db.one("select id from medora.notification where id=? and facility_id=? and recipient_id=? and status='DELIVERED'",id,facility,access.actor());
        db.update("update medora.notification set read_at=coalesce(read_at,current_timestamp) where id=?",id); return db.one("select * from medora.notification where id=?",id);
    }
    public List<Map<String,Object>> status(UUID facility,int page,int size) {
        access.require(facility,"notification.send"); page(page,size,"id");
        return db.list("select * from medora.notification where facility_id=? order by created_at desc,id limit ? offset ?",facility,size,(long)page*size);
    }
}
