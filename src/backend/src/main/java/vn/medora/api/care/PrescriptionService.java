package vn.medora.api.care;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.medora.api.common.*;
import java.math.BigDecimal;
import java.util.*;
import static vn.medora.api.common.ApiSupport.*;
import static vn.medora.api.common.SqlRows.*;
import static vn.medora.api.common.ClinicAccess.*;
import static vn.medora.api.scheduling.SchedulingService.expected;
import static vn.medora.api.care.CareDtos.*;

@Service @Transactional
public class PrescriptionService {
    private final Store db; private final ClinicAccess access; private final AuditService audit; private final RequestDeduplication dedup;
    public PrescriptionService(Store db,ClinicAccess access,AuditService audit,RequestDeduplication dedup) {
        this.db=db; this.access=access; this.audit=audit; this.dedup=dedup;
    }
    private Map<String,Object> rx(UUID facility,UUID id) { return db.one("select * from medora.prescription where id=? and facility_id=?",id,facility); }
    private List<Map<String,Object>> items(UUID id) { return db.list("select * from medora.prescription_item where prescription_id=? order by id",id); }
    private Map<String,Object> view(Map<String,Object> row) {
        var result=new LinkedHashMap<>(row); result.put("items",items(uuid(row,"id"))); return result;
    }
    private Map<String,Object> doctor(UUID facility,Map<String,Object> row) {
        var visit=access.doctor(facility,uuid(row,"encounterId"),true); access.open(visit);
        state(visit,"IN_CONSULTATION","AWAITING_SERVICE"); return visit;
    }
    public List<Map<String,Object>> list(UUID facility,UUID encounter) {
        access.encounter(facility,encounter,"clinical.read",false);
        audit.record(access.actor(),facility,"PRESCRIPTIONS_READ","ENCOUNTER",encounter);
        return db.list("select * from medora.prescription where facility_id=? and encounter_id=? order by created_at desc limit 100",facility,encounter).stream().map(this::view).toList();
    }
    public List<Map<String,Object>> pharmacy(UUID facility,int page,int size) {
        UUID actor=access.require(facility,"prescription.read"); page(page,size,"id");
        return db.list("select * from medora.prescription where facility_id=? and pharmacist_id=? order by created_at desc limit ? offset ?",facility,actor,size,(long)page*size)
            .stream().map(this::view).toList();
    }
    public Map<String,Object> get(UUID facility,UUID id,boolean export) {
        var row=rx(facility,id);
        if(access.permits(facility,"prescription.read") && access.actor().equals(row.get("pharmacistId"))) { }
        else { access.encounter(facility,uuid(row,"encounterId"),"clinical.read",false); }
        audit.record(access.actor(),facility,export?"PRESCRIPTION_EXPORTED":"PRESCRIPTION_READ","PRESCRIPTION",id);
        return view(row);
    }
    public Map<String,Object> create(UUID facility,UUID encounter,RxInput r) {
        access.require(facility,"clinical.write"); access.lock(facility);
        var visit=access.doctor(facility,encounter,true); access.open(visit); state(visit,"IN_CONSULTATION","AWAITING_SERVICE");
        UUID id=UUID.randomUUID(); db.update("insert into medora.prescription(id,facility_id,encounter_id) values(?,?,?)",id,facility,encounter);
        replaceItems(facility,id,r.items()); record(facility,id,"PRESCRIPTION_DRAFT_CREATED",null); return view(rx(facility,id));
    }
    public Map<String,Object> save(UUID facility,UUID id,RxInput r) {
        access.require(facility,"clinical.write"); access.lock(facility); var row=rx(facility,id); doctor(facility,row);
        state(row,"DRAFT"); expected(row,r.version()); replaceItems(facility,id,r.items());
        db.update("update medora.prescription set version=version+1 where id=?",id);
        record(facility,id,"PRESCRIPTION_DRAFT_SAVED",null); return view(rx(facility,id));
    }
    private void replaceItems(UUID facility,UUID id,List<RxItem> input) {
        if(input.stream().map(RxItem::medicationId).distinct().count()!=input.size()) { throw bad("DUPLICATE_MEDICATION_ITEM"); }
        db.update("delete from medora.prescription_item where prescription_id=?",id);
        for(var r:input) {
            var medication=db.one("select * from medora.medication where id=? and facility_id=? and enabled",r.medicationId(),facility);
            db.update("""
                insert into medora.prescription_item(id,prescription_id,medication_id,medication_name,ingredient_code,unit,dose,route,frequency,duration_days,quantity,instructions)
                values(?,?,?,?,?,?,?,?,?,?,?,?)
                """,UUID.randomUUID(),id,r.medicationId(),medication.get("name"),medication.get("ingredientCode"),medication.get("unit"),
                r.dose().strip(),r.route().strip(),r.frequency().strip(),r.durationDays(),r.quantity(),r.instructions().strip());
        }
    }
    public Map<String,Object> safety(UUID facility,UUID id) {
        var row=rx(facility,id); access.encounter(facility,uuid(row,"encounterId"),"clinical.read",false);
        var result=warnings(facility,row); record(facility,id,"MEDICATION_SAFETY_CHECKED",null); return result;
    }
    private Set<String> codes(Object field) {
        String text=Objects.toString(field,""); return text.isEmpty()?new HashSet<>():new HashSet<>(Arrays.asList(text.split(",")));
    }
    private Map<String,Object> warnings(UUID facility,Map<String,Object> row) {
        var policy=db.optional("select * from medora.medication_policy where facility_id=? and enabled",facility)
            .orElseThrow(()->conflict("SAFETY_POLICY_NOT_CONFIGURED"));
        var visit=db.one("select patient_id from medora.encounter where id=?",row.get("encounterId"));
        var profile=db.optional("select * from medora.medication_profile where patient_id=? and facility_id=?",visit.get("patientId"),facility)
            .orElseThrow(()->conflict("MEDICATION_PROFILE_REQUIRED"));
        if(profile.get("allergyStatus").equals("UNKNOWN")) { throw conflict("ALLERGY_STATUS_UNKNOWN"); }
        var selected=items(uuid(row,"id")); if(selected.isEmpty()) { throw conflict("PRESCRIPTION_ITEMS_REQUIRED"); }
        Set<String> allergies=codes(profile.get("allergyCodes")),conditions=codes(profile.get("conditionCodes"));
        Set<String> medications=codes(profile.get("currentMedicationIds"));
        selected.forEach(i->medications.add(i.get("medicationId").toString()));
        db.list("""
            select i.medication_id from medora.prescription p join medora.prescription_item i on i.prescription_id=p.id
            join medora.encounter e on e.id=p.encounter_id where p.facility_id=? and e.patient_id=? and p.status='ISSUED'
            and p.issued_at + i.duration_days * interval '1 day' >= current_timestamp and p.id<>?
            """,facility,visit.get("patientId"),row.get("id")).forEach(i->medications.add(i.get("medicationId").toString()));
        List<Map<String,Object>> warnings=new ArrayList<>();
        for(var item:selected) {
            var current=db.one("select * from medora.medication where id=? and facility_id=? and enabled",item.get("medicationId"),facility);
            if(!current.get("ingredientCode").equals(item.get("ingredientCode"))) { throw conflict("MEDICATION_CHANGED_REFRESH_DRAFT"); }
            if(allergies.contains(item.get("ingredientCode"))) {
                warnings.add(Map.of("id","ALLERGY:"+item.get("ingredientCode"),"severity","BLOCK","overridable",false,
                    "message","Recorded ingredient allergy","source","Recorded structured patient allergy profile"));
            }
        }
        Set<String> selectedIds=new HashSet<>(); selected.forEach(i->selectedIds.add(i.get("medicationId").toString()));
        for(var rule:db.list("select * from medora.medication_rule where facility_id=? and enabled order by id",facility)) {
            boolean matches;
            if(rule.get("kind").equals("INTERACTION")) {
                String first=rule.get("medicationId").toString(),second=rule.get("otherMedicationId").toString();
                matches=medications.contains(first)&&medications.contains(second)&&(selectedIds.contains(first)||selectedIds.contains(second));
            } else { matches=selectedIds.contains(rule.get("medicationId").toString())&&conditions.contains(rule.get("conditionCode")); }
            if(matches) { warnings.add(Map.of("id",rule.get("id").toString(),"severity",rule.get("severity"),
                "overridable",rule.get("overridable"),"message",rule.get("message"),"source",rule.get("source"))); }
        }
        return Map.of("source",policy.get("source"),"dataVersion",policy.get("dataVersion"),"warnings",warnings);
    }
    public Map<String,Object> issue(UUID facility,UUID id,String key,IssueInput r) {
        UUID actor=access.require(facility,"clinical.write"); access.lock(facility);
        var row=rx(facility,id); access.doctor(facility,uuid(row,"encounterId"),true);
        var fingerprint=Map.of("prescriptionId",id,"input",r);
        var previous=dedup.previous(facility,actor,"RX_ISSUE",key,fingerprint); if(previous.isPresent()) { return previous.get(); }
        doctor(facility,row); state(row,"DRAFT"); expected(row,r.version()); access.staff(facility,r.pharmacistId(),"PHARMACIST");
        var safety=warnings(facility,row);
        @SuppressWarnings("unchecked") var warnings=(List<Map<String,Object>>)safety.get("warnings");
        Set<String> expectedWarnings=new HashSet<>();
        for(var warning:warnings) {
            expectedWarnings.add(warning.get("id").toString());
            if(warning.get("severity").equals("BLOCK")) {
                if(!Boolean.TRUE.equals(warning.get("overridable"))) { throw conflict("NON_OVERRIDABLE_SAFETY_WARNING"); }
                access.require(facility,"prescription.override"); reason(r.overrideReason());
            }
        }
        if(!r.acknowledgedWarnings().equals(expectedWarnings)) { throw conflict("SAFETY_WARNINGS_REQUIRE_REVIEW"); }
        if(row.get("predecessorId")!=null) {
            var predecessor=rx(facility,uuid(row,"predecessorId")); state(predecessor,"ISSUED");
            db.update("update medora.prescription set status='SUPERSEDED',version=version+1 where id=?",row.get("predecessorId"));
        }
        db.update("""
            update medora.prescription set status='ISSUED',pharmacist_id=?,issued_by=?,issued_at=current_timestamp,
            safety_source=?,safety_version=?,version=version+1 where id=?
            """,r.pharmacistId(),actor,safety.get("source"),safety.get("dataVersion"),id);
        record(facility,id,"PRESCRIPTION_ISSUED",optional(r.overrideReason()));
        return dedup.save(facility,actor,"RX_ISSUE",key,fingerprint,view(rx(facility,id)));
    }
    public Map<String,Object> amend(UUID facility,UUID id,ReasonVersion r) {
        access.require(facility,"clinical.write"); access.lock(facility); var old=rx(facility,id); doctor(facility,old);
        expected(old,r.version()); state(old,"ISSUED"); reason(r.reason());
        UUID next=UUID.randomUUID(); db.update("insert into medora.prescription(id,facility_id,encounter_id,predecessor_id,reason) values(?,?,?,?,?)",
            next,facility,old.get("encounterId"),id,r.reason());
        for(var item:items(id)) {
            db.update("""
                insert into medora.prescription_item(id,prescription_id,medication_id,medication_name,ingredient_code,unit,dose,route,frequency,duration_days,quantity,instructions)
                values(?,?,?,?,?,?,?,?,?,?,?,?)
                """,UUID.randomUUID(),next,item.get("medicationId"),item.get("medicationName"),item.get("ingredientCode"),item.get("unit"),
                item.get("dose"),item.get("route"),item.get("frequency"),item.get("durationDays"),item.get("quantity"),item.get("instructions"));
        }
        record(facility,next,"PRESCRIPTION_AMENDMENT_CREATED",r.reason()); return view(rx(facility,next));
    }
    public Map<String,Object> cancel(UUID facility,UUID id,ReasonVersion r) {
        access.require(facility,"clinical.write"); access.lock(facility); var row=rx(facility,id); doctor(facility,row);
        expected(row,r.version()); state(row,"ISSUED"); reason(r.reason());
        db.update("update medora.prescription set status='CANCELED',cancel_reason=?,canceled_by=?,canceled_at=current_timestamp,version=version+1 where id=?",r.reason(),access.actor(),id);
        record(facility,id,"PRESCRIPTION_CANCELED",r.reason()); return view(rx(facility,id));
    }
    public Map<String,Object> medicineEvent(UUID facility,UUID id,String key,MedicineEventInput r) {
        UUID actor=access.require(facility,"prescription.dispense"); access.lock(facility); var row=rx(facility,id);
        if(!actor.equals(row.get("pharmacistId"))) { throw new AccessDeniedException("PRESCRIPTION_NOT_ASSIGNED"); }
        var fingerprint=Map.of("prescriptionId",id,"input",r);
        var previous=dedup.previous(facility,actor,"MEDICINE_EVENT",key,fingerprint); if(previous.isPresent()) { return previous.get(); }
        state(row,"ISSUED");
        var item=db.one("select * from medora.prescription_item where id=? and prescription_id=?",r.itemId(),id);
        if(r.performedAt().isBefore(instant(row,"issuedAt"))) { throw bad("EVENT_PREDATES_PRESCRIPTION"); }
        var total=db.one("select coalesce(sum(quantity),0) as quantity from medora.medication_event where item_id=? and kind=?",r.itemId(),r.kind());
        if(((BigDecimal)total.get("quantity")).add(r.quantity()).compareTo((BigDecimal)item.get("quantity"))>0) { throw conflict("MEDICINE_QUANTITY_EXCEEDED"); }
        UUID event=UUID.randomUUID(); db.update("""
            insert into medora.medication_event(id,facility_id,prescription_id,item_id,kind,quantity,performed_by,performed_at) values(?,?,?,?,?,?,?,?)
            """,event,facility,id,r.itemId(),r.kind(),r.quantity(),actor,r.performedAt());
        record(facility,id,"MEDICINE_EVENT_RECORDED",null);
        return dedup.save(facility,actor,"MEDICINE_EVENT",key,fingerprint,db.one("select * from medora.medication_event where id=?",event));
    }
    private void record(UUID facility,UUID id,String action,String detail) {
        audit.record(access.actor(),facility,action,"PRESCRIPTION",id,null,detail);
    }
}
