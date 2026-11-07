package vn.medora.api.care;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.medora.api.common.*;
import java.util.*;
import static vn.medora.api.common.ApiSupport.*;
import static vn.medora.api.common.SqlRows.*;
import static vn.medora.api.scheduling.SchedulingService.expected;
import static vn.medora.api.care.CareDtos.*;

@Service @Transactional
public class MedicationCatalogService {
    private final Store db; private final ClinicAccess access; private final AuditService audit;
    public MedicationCatalogService(Store db, ClinicAccess access, AuditService audit) {
        this.db=db; this.access=access; this.audit=audit;
    }
    private void reader(UUID facility) {
        if (!access.permits(facility,"clinical.read") && !access.permits(facility,"catalog.manage")
            && !access.permits(facility,"prescription.read")) { throw new AccessDeniedException("FORBIDDEN"); }
    }
    public List<Map<String,Object>> medicines(UUID facility, String q, int page, int size) {
        reader(facility); page(page,size,"id"); if(q.length()>160) { throw bad("SEARCH_TOO_LONG"); }
        return db.list("select * from medora.medication where facility_id=? and (lower(name) like ? escape '!' or lower(code) like ? escape '!') order by code limit ? offset ?",
            facility,like(q),like(q),size,(long)page*size);
    }
    public Map<String,Object> medicine(UUID facility, UUID id, MedicationInput r) {
        UUID actor=access.require(facility,"catalog.manage"); access.lock(facility);
        if(id==null) {
            id=UUID.randomUUID(); db.update("insert into medora.medication(id,facility_id,code,name,ingredient_code,unit,enabled) values(?,?,?,?,?,?,?)",
                id,facility,r.code(),r.name().strip(),r.ingredientCode(),r.unit().strip(),r.enabled());
        } else {
            var previous=db.one("select * from medora.medication where id=? and facility_id=?",id,facility); expected(previous,r.version());
            db.update("update medora.medication set code=?,name=?,ingredient_code=?,unit=?,enabled=?,version=version+1 where id=?",
                r.code(),r.name().strip(),r.ingredientCode(),r.unit().strip(),r.enabled(),id);
        }
        audit.record(actor,facility,"MEDICATION_CATALOG_SAVED","MEDICATION",id);
        return db.one("select * from medora.medication where id=?",id);
    }
    public List<Map<String,Object>> rules(UUID facility) {
        access.require(facility,"catalog.manage"); return db.list("select * from medora.medication_rule where facility_id=? order by id limit 100",facility);
    }
    public Map<String,Object> rule(UUID facility, UUID id, RuleInput r) {
        UUID actor=access.require(facility,"catalog.manage"); access.lock(facility);
        db.one("select id from medora.medication where id=? and facility_id=?",r.medicationId(),facility);
        if(r.kind().equals("INTERACTION")) {
            if(r.otherMedicationId()==null || r.otherMedicationId().equals(r.medicationId())) { throw bad("INVALID_INTERACTION"); }
            db.one("select id from medora.medication where id=? and facility_id=?",r.otherMedicationId(),facility);
        } else if(optional(r.conditionCode())==null) { throw bad("CONDITION_CODE_REQUIRED"); }
        if(id==null) {
            id=UUID.randomUUID(); db.update("""
                insert into medora.medication_rule(id,facility_id,medication_id,other_medication_id,kind,condition_code,severity,overridable,message,source,enabled)
                values(?,?,?,?,?,?,?,?,?,?,?)
                """,id,facility,r.medicationId(),r.otherMedicationId(),r.kind(),r.conditionCode(),r.severity(),r.overridable(),r.message(),r.source(),r.enabled());
        } else {
            expected(db.one("select * from medora.medication_rule where id=? and facility_id=?",id,facility),r.version());
            db.update("""
                update medora.medication_rule set medication_id=?,other_medication_id=?,kind=?,condition_code=?,severity=?,overridable=?,message=?,source=?,enabled=?,version=version+1 where id=?
                """,r.medicationId(),r.otherMedicationId(),r.kind(),r.conditionCode(),r.severity(),r.overridable(),r.message(),r.source(),r.enabled(),id);
        }
        audit.record(actor,facility,"MEDICATION_RULE_SAVED","MEDICATION_RULE",id);
        return db.one("select * from medora.medication_rule where id=?",id);
    }
    public Map<String,Object> policy(UUID facility, SafetyPolicyInput r) {
        UUID actor=access.require(facility,"catalog.manage"); access.lock(facility);
        var previous=db.optional("select * from medora.medication_policy where facility_id=?",facility);
        if(previous.isPresent()) { expected(previous.get(),r.version()); }
        db.update("""
            insert into medora.medication_policy(facility_id,source,data_version,enabled,approved_by) values(?,?,?,?,?)
            on conflict(facility_id) do update set source=excluded.source,data_version=excluded.data_version,enabled=excluded.enabled,
                approved_by=excluded.approved_by,approved_at=current_timestamp,version=medora.medication_policy.version+1
            """,facility,r.source(),r.dataVersion(),r.enabled(),actor);
        audit.record(actor,facility,"MEDICATION_POLICY_SAVED","FACILITY",facility);
        return db.one("select * from medora.medication_policy where facility_id=?",facility);
    }
    public Map<String,Object> policy(UUID facility) {
        reader(facility); return db.one("select * from medora.medication_policy where facility_id=?",facility);
    }
    public Map<String,Object> profile(UUID facility, UUID encounter, ProfileInput r) {
        access.require(facility,"history.write"); access.lock(facility);
        var visit=access.encounter(facility,encounter,"history.write",true); access.open(visit);
        if(r.allergyStatus().equals("KNOWN_NONE") && !r.allergyCodes().isEmpty()
            || r.allergyStatus().equals("RECORDED") && r.allergyCodes().isEmpty()) { throw bad("ALLERGY_STATUS_MISMATCH"); }
        for(UUID medicine:r.currentMedicationIds()) { db.one("select id from medora.medication where id=? and facility_id=?",medicine,facility); }
        UUID patient=uuid(visit,"patientId");
        var previous=db.optional("select * from medora.medication_profile where patient_id=? and facility_id=?",patient,facility);
        if(previous.isPresent()) { expected(previous.get(),r.version()); }
        db.update("""
            insert into medora.medication_profile(patient_id,facility_id,allergy_status,allergy_codes,condition_codes,current_medication_ids,recorded_by)
            values(?,?,?,?,?,?,?) on conflict(patient_id) do update set allergy_status=excluded.allergy_status,
            allergy_codes=excluded.allergy_codes,condition_codes=excluded.condition_codes,current_medication_ids=excluded.current_medication_ids,
            recorded_by=excluded.recorded_by,recorded_at=current_timestamp,version=medora.medication_profile.version+1
            """,patient,facility,r.allergyStatus(),String.join(",",new TreeSet<>(r.allergyCodes())),String.join(",",new TreeSet<>(r.conditionCodes())),
            r.currentMedicationIds().stream().map(UUID::toString).sorted().collect(java.util.stream.Collectors.joining(",")),access.actor());
        audit.record(access.actor(),facility,"MEDICATION_PROFILE_RECORDED","ENCOUNTER",encounter);
        return db.one("select * from medora.medication_profile where patient_id=?",patient);
    }
    public Map<String,Object> profile(UUID facility,UUID encounter) {
        var visit=access.encounter(facility,encounter,"clinical.read",false); access.open(visit);
        audit.record(access.actor(),facility,"MEDICATION_PROFILE_READ","ENCOUNTER",encounter);
        return db.one("select * from medora.medication_profile where patient_id=? and facility_id=?",visit.get("patientId"),facility);
    }
}
