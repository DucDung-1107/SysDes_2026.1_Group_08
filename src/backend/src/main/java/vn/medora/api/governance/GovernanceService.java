package vn.medora.api.governance;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.medora.api.common.*;
import vn.medora.api.finance.FinanceDtos.PolicyInput;
import java.time.*;
import java.util.*;
import static vn.medora.api.common.ApiSupport.*;
import static vn.medora.api.common.SqlRows.*;
import static vn.medora.api.common.ClinicAccess.*;
import static vn.medora.api.scheduling.SchedulingService.expected;
import static vn.medora.api.governance.GovernanceDtos.*;

@Service @Transactional
public class GovernanceService {
    private final Store db; private final ClinicAccess access; private final AuditService audit; private final RequestDeduplication dedup;
    public GovernanceService(Store db,ClinicAccess access,AuditService audit,RequestDeduplication dedup) { this.db=db; this.access=access; this.audit=audit; this.dedup=dedup; }
    public Map<String,Object> policy(UUID facility) {
        access.require(facility,"policy.manage");
        db.update("insert into medora.clinic_policy(facility_id) values(?) on conflict do nothing",facility);
        return db.one("select * from medora.clinic_policy where facility_id=?",facility);
    }
    public Map<String,Object> diagnosis(UUID facility,DiagnosisInput r) {
        UUID actor=access.require(facility,"catalog.manage"); access.lock(facility);
        var existing=db.optional("select * from medora.diagnosis_catalog where code=?",r.code());
        if(existing.isPresent() && !facility.equals(existing.get().get("facilityId"))) { throw conflict("DIAGNOSIS_CODE_UNAVAILABLE"); }
        db.update("insert into medora.diagnosis_catalog(code,name,enabled,facility_id) values(?,?,?,?) on conflict(code) do update set name=excluded.name,enabled=excluded.enabled",
            r.code(),r.name(),r.enabled(),facility);
        audit.record(actor,facility,"DIAGNOSIS_CATALOG_SAVED","FACILITY",facility);
        return db.one("select * from medora.diagnosis_catalog where code=? and facility_id=?",r.code(),facility);
    }
    public Map<String,Object> policy(UUID facility,PolicyInput r) {
        UUID actor=access.require(facility,"policy.manage"); access.lock(facility); expected(policy(facility),r.version());
        db.update("update medora.clinic_policy set auto_refund_limit=?,discharge_approval_required=?,version=version+1 where facility_id=?",
            r.autoRefundLimit(),r.dischargeApprovalRequired(),facility);
        audit.record(actor,facility,"CLINIC_POLICY_UPDATED","FACILITY",facility); return policy(facility);
    }
    public Map<String,Object> mergeRequest(UUID facility,String key,MergeInput r) {
        UUID actor=access.require(facility,"merge.request"); access.lock(facility);
        var old=dedup.previous(facility,actor,"MERGE_REQUEST",key,r); if(old.isPresent()) { return old.get(); }
        if(r.sourceId().equals(r.targetId())) { throw bad("CANNOT_MERGE_SAME_PATIENT"); }
        access.activePatient(facility,r.sourceId()); access.activePatient(facility,r.targetId());
        UUID id=UUID.randomUUID(); db.update("insert into medora.merge_request(id,facility_id,source_id,target_id,reason,requested_by) values(?,?,?,?,?,?)",
            id,facility,r.sourceId(),r.targetId(),r.reason(),actor);
        audit.record(actor,facility,"PATIENT_MERGE_REQUESTED","MERGE_REQUEST",id);
        return dedup.save(facility,actor,"MERGE_REQUEST",key,r,db.one("select * from medora.merge_request where id=?",id));
    }
    public List<Map<String,Object>> merges(UUID facility,int page,int size) {
        if(!access.permits(facility,"merge.execute")) { access.require(facility,"merge.request"); }
        page(page,size,"id"); return db.list("select * from medora.merge_request where facility_id=? order by created_at desc,id limit ? offset ?",facility,size,(long)page*size);
    }
    public Map<String,Object> decideMerge(UUID facility,UUID id,String key,Decision r) {
        UUID actor=access.require(facility,"merge.execute"); access.lock(facility);
        var fingerprint=Map.of("mergeRequestId",id,"input",r); var old=dedup.previous(facility,actor,"MERGE_DECISION",key,fingerprint); if(old.isPresent()) { return old.get(); }
        var row=db.one("select * from medora.merge_request where id=? and facility_id=? for update",id,facility); expected(row,r.version()); state(row,"PENDING");
        if(r.action().equals("APPROVE")) {
            if(!Boolean.TRUE.equals(r.identityVerified())) { throw bad("IDENTITY_VERIFICATION_REQUIRED"); }
            UUID source=uuid(row,"sourceId"),target=uuid(row,"targetId");
            access.activePatient(facility,source); access.activePatient(facility,target);
            db.list("select id from medora.patient where facility_id=? and id in (?,?) order by id for update",facility,source,target);
            if(db.exists("select exists(select 1 from medora.encounter where facility_id=? and patient_id in (?,?) and status<>'COMPLETED')",facility,source,target)
                || db.exists("select exists(select 1 from medora.appointment where facility_id=? and patient_id=? and status in ('BOOKED','CONFIRMED'))",facility,source)) {
                throw conflict("RESOLVE_ACTIVE_WORK_BEFORE_MERGE");
            }
            db.update("update medora.patient set merged_into_id=?,version=version+1 where id=?",target,source);
            // Never move or rewrite signed clinical or financial evidence. Reconcile safety inputs anew.
            db.update("update medora.medication_profile set allergy_status='UNKNOWN',version=version+1 where patient_id=?",target);
            db.update("update medora.merge_request set status='COMPLETED',verification_reason=?,resolved_by=?,resolved_at=current_timestamp,version=version+1 where id=?",r.reason(),actor,id);
        } else {
            db.update("update medora.merge_request set status='REJECTED',verification_reason=?,resolved_by=?,resolved_at=current_timestamp,version=version+1 where id=?",r.reason(),actor,id);
        }
        audit.record(actor,facility,"PATIENT_MERGE_"+r.action(),"MERGE_REQUEST",id);
        return dedup.save(facility,actor,"MERGE_DECISION",key,fingerprint,db.one("select * from medora.merge_request where id=?",id));
    }
    public Map<String,Object> discharge(UUID facility,UUID encounter,Reason r) {
        access.require(facility,"clinical.write"); access.lock(facility); var visit=access.doctor(facility,encounter,true); access.open(visit);
        UUID id=UUID.randomUUID(); db.update("insert into medora.discharge_request(id,facility_id,encounter_id,reason,requested_by) values(?,?,?,?,?)",id,facility,encounter,r.reason(),access.actor());
        audit.record(access.actor(),facility,"DISCHARGE_REQUESTED","DISCHARGE_REQUEST",id); return db.one("select * from medora.discharge_request where id=?",id);
    }
    public Map<String,Object> decideDischarge(UUID facility,UUID id,DischargeDecision r) {
        UUID actor=access.require(facility,"discharge.approve"); access.lock(facility);
        var row=db.one("select * from medora.discharge_request where id=? and facility_id=? for update",id,facility); state(row,"PENDING"); expected(row,r.version());
        if(actor.equals(row.get("requestedBy"))) { throw new org.springframework.security.access.AccessDeniedException("SELF_APPROVAL_NOT_ALLOWED"); }
        db.update("update medora.discharge_request set status=?,resolved_by=?,resolved_at=current_timestamp,decision_reason=?,version=version+1 where id=?",
            r.action().equals("APPROVE")?"APPROVED":"REJECTED",actor,r.reason(),id);
        audit.record(actor,facility,"DISCHARGE_"+r.action(),"DISCHARGE_REQUEST",id); return db.one("select * from medora.discharge_request where id=?",id);
    }
    public List<Map<String,Object>> audits(UUID facility,Instant from,Instant to,UUID actor,UUID target,String action,int page,int size) {
        UUID reader=access.require(facility,"audit.read"); page(page,size,"id"); range(from,to);
        if(action!=null&&!action.matches("[A-Z0-9_]{1,64}")) { throw bad("INVALID_AUDIT_ACTION"); }
        var result=db.list("""
            select * from (
                select id,actor_id,action,target_type,target_id,related_id,detail,occurred_at from medora.audit_event where facility_id=?
                union all select id,actor_account_id,action,'USER',target_account_id,null,detail,occurred_at from medora.identity_audit_event where facility_id=?
            ) a where occurred_at>=? and occurred_at<? and (cast(? as uuid) is null or actor_id=?)
                and (cast(? as uuid) is null or target_id=?) and (cast(? as varchar) is null or action=?)
            order by occurred_at desc,id limit ? offset ?
            """,facility,facility,from,to,actor,actor,target,target,action,action,size,(long)page*size);
        audit.record(reader,facility,"AUDIT_LOG_READ","FACILITY",facility); return result;
    }
    public List<Map<String,Object>> patientAudit(UUID facility,UUID patient,int page,int size) {
        UUID actor=access.require(facility,"audit.read"); page(page,size,"id"); db.one("select id from medora.patient where id=? and facility_id=?",patient,facility);
        var result=db.list("""
            with recursive patients as (select id from medora.patient where id=? and facility_id=? union all
                select p.id from medora.patient p join patients t on p.merged_into_id=t.id where p.facility_id=?),
            visits as (select id from medora.encounter where patient_id in(select id from patients) and facility_id=?),
            invoices as (select id from medora.invoice where encounter_id in(select id from visits)),
            payments as (select id from medora.payment where invoice_id in(select id from invoices)),
            targets as (select id from patients union select id from visits union select id from invoices union select id from payments
                union select id from medora.refund where payment_id in(select id from payments)
                union select id from medora.prescription where encounter_id in(select id from visits)
                union select id from medora.procedure_order where encounter_id in(select id from visits)
                union select id from medora.attachment where encounter_id in(select id from visits))
            select * from medora.audit_event where facility_id=? and (target_id in(select id from targets) or related_id in(select id from targets))
            order by occurred_at desc,id limit ? offset ?
            """,patient,facility,facility,facility,facility,size,(long)page*size);
        audit.record(actor,facility,"PATIENT_AUDIT_READ","PATIENT",patient); return result;
    }
    public static void range(Instant from,Instant to) {
        if(!to.isAfter(from)||Duration.between(from,to).compareTo(Duration.ofDays(366))>0) { throw bad("INVALID_DATE_RANGE"); }
    }
}
