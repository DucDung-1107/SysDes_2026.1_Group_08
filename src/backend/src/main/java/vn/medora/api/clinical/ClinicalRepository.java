package vn.medora.api.clinical;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.*;
import static vn.medora.api.common.SqlRows.*;
import static vn.medora.api.common.ApiSupport.*;
import static vn.medora.api.clinical.ClinicalDtos.*;

@Repository
public class ClinicalRepository {
    private final JdbcTemplate jdbc;
    public ClinicalRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    private Map<String, Object> one(String sql, Object... params) {
        return jdbc.query(sql, MAPPER, params).stream().findFirst().orElseThrow(() -> missing());
    }
    public Map<String, Object> encounter(UUID facility, UUID id, boolean lock) {
        return one("select * from medora.encounter where facility_id=? and id=?" + (lock ? " for update" : ""), facility, id);
    }
    public boolean activeEncounter(UUID facility, UUID patient) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
            select exists(select 1 from medora.encounter where facility_id=? and patient_id=? and status<>'COMPLETED')
            """, Boolean.class, facility, patient));
    }
    public Map<String, Object> checkIn(UUID facility, UUID patient, UUID appointment, UUID doctor, UUID nurse) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
            insert into medora.encounter(id,facility_id,patient_id,appointment_id,doctor_id,nurse_id) values(?,?,?,?,?,?)
            """, id, facility, patient, appointment, doctor, nurse);
        return encounter(facility, id, false);
    }
    public List<Map<String, Object>> queue(UUID facility, UUID actor, boolean reception, int page, int size) {
        return jdbc.query("""
            select e.id,e.patient_id,e.appointment_id,e.doctor_id,e.nurse_id,e.status,e.queue_number,e.version,
                p.code as patient_code,p.full_name as patient_name,e.created_at
            from medora.encounter e join medora.patient p on p.id=e.patient_id
            where e.facility_id=? and e.status<>'COMPLETED' and (? or e.doctor_id=? or e.nurse_id=?)
            order by e.queue_number limit ? offset ?
            """, MAPPER, facility, reception, actor, actor, size, (long) page * size);
    }
    public void assignNurse(UUID id, UUID nurse) {
        jdbc.update("update medora.encounter set nurse_id=?,version=version+1 where id=?", nurse, id);
    }
    public void state(UUID id, String status) {
        jdbc.update("update medora.encounter set status=?,version=version+1 where id=?", status, id);
    }
    public void complete(UUID id, UUID actor, CloseInput r) {
        jdbc.update("""
            update medora.encounter set status='COMPLETED',completed_at=current_timestamp,completed_by=?,
                summary=?,instructions=?,follow_up=?,version=version+1 where id=?
            """, actor, r.summary().strip(), r.instructions().strip(), r.followUp().strip(), id);
    }
    public List<Map<String, Object>> history(UUID facility, UUID patient, int page, int size) {
        return jdbc.query("""
            select h.*,exists(select 1 from medora.patient_history n where n.supersedes_id=h.id) as superseded
            from medora.patient_history h where h.facility_id=? and h.patient_id in (
                with recursive tree as (select id from medora.patient where id=? union all
                select p.id from medora.patient p join tree t on p.merged_into_id=t.id) select id from tree)
            order by h.recorded_at desc,h.id limit ? offset ?
            """, MAPPER, facility, patient, size, (long) page * size);
    }
    public Map<String, Object> historyEntry(UUID facility, UUID id) {
        return one("select * from medora.patient_history where facility_id=? and id=?", facility, id);
    }
    public Map<String, Object> addHistory(UUID facility, UUID encounter, UUID patient, UUID actor, HistoryInput r) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
            insert into medora.patient_history(id,facility_id,patient_id,encounter_id,kind,content,recorded_by,supersedes_id,correction_reason)
            values(?,?,?,?,?,?,?,?,?)
            """, id, facility, patient, encounter, r.kind(), r.content().strip(), actor, r.supersedesId(), r.correctionReason());
        return historyEntry(facility, id);
    }
    public List<Map<String, Object>> timeline(UUID facility, UUID patient, int page, int size) {
        return jdbc.query("""
            select id,patient_id,doctor_id,status,queue_number,created_at,completed_at,summary,instructions,follow_up
            from medora.encounter where facility_id=? and patient_id in (
                with recursive tree as (select id from medora.patient where id=? union all
                select p.id from medora.patient p join tree t on p.merged_into_id=t.id) select id from tree)
            order by created_at desc,id limit ? offset ?
            """, MAPPER, facility, patient, size, (long) page * size);
    }
    public List<Map<String, Object>> vitals(UUID facility, UUID encounter, int page, int size) {
        return jdbc.query("""
            select v.*,exists(select 1 from medora.vital_sign n where n.supersedes_id=v.id) as superseded
            from medora.vital_sign v where v.facility_id=? and v.encounter_id=?
            order by v.recorded_at desc,v.id limit ? offset ?
            """, MAPPER, facility, encounter, size, (long) page * size);
    }
    public Map<String, Object> vital(UUID facility, UUID id) {
        return one("select * from medora.vital_sign where facility_id=? and id=?", facility, id);
    }
    public Map<String, Object> addVitals(UUID facility, UUID encounter, UUID actor, VitalInput r) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
            insert into medora.vital_sign(id,facility_id,encounter_id,temperature_c,pulse_bpm,systolic_mmhg,diastolic_mmhg,
                respiratory_rate,spo2_percent,weight_kg,height_cm,screening,measured_at,recorded_by,verified,supersedes_id,correction_reason)
            values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """, id, facility, encounter, r.temperatureC(), r.pulseBpm(), r.systolicMmhg(), r.diastolicMmhg(),
            r.respiratoryRate(), r.spo2Percent(), r.weightKg(), r.heightCm(), r.screening(), time(r.measuredAt()),
            actor, r.verified(), r.supersedesId(), r.correctionReason());
        return vital(facility, id);
    }
    public boolean verifiedVitals(UUID encounter) {
        return jdbc.query("""
            select verified from medora.vital_sign v where encounter_id=?
            and not exists(select 1 from medora.vital_sign n where n.supersedes_id=v.id)
            order by recorded_at desc,id limit 1
            """, (rs, n) -> rs.getBoolean(1), encounter).stream().findFirst().orElse(false);
    }
    public List<Map<String, Object>> diagnosisCatalog(UUID facility) {
        return jdbc.query("select code,name from medora.diagnosis_catalog where enabled and (facility_id is null or facility_id=?) order by code limit 100", MAPPER, facility);
    }
    public List<Map<String, Object>> diagnoses(UUID facility, UUID encounter) {
        return jdbc.query("select * from medora.encounter_diagnosis where facility_id=? and encounter_id=? order by recorded_at,id limit 100",
            MAPPER, facility, encounter);
    }
    public Map<String, Object> addDiagnosis(UUID facility, UUID encounter, UUID actor, DiagnosisInput r) {
        boolean enabled = Boolean.TRUE.equals(jdbc.queryForObject(
            "select exists(select 1 from medora.diagnosis_catalog where code=? and enabled and (facility_id is null or facility_id=?))", Boolean.class, r.code(), facility));
        if (!enabled) { throw bad("DIAGNOSIS_CODE_NOT_ENABLED"); }
        UUID id = UUID.randomUUID();
        jdbc.update("""
            insert into medora.encounter_diagnosis(id,facility_id,encounter_id,code,symptoms,recorded_by) values(?,?,?,?,?,?)
            """, id, facility, encounter, r.code(), r.symptoms().strip(), actor);
        return one("select * from medora.encounter_diagnosis where id=?", id);
    }
    public List<Map<String, Object>> notes(UUID facility, UUID encounter, int page, int size) {
        return jdbc.query("""
            select * from medora.clinical_note where facility_id=? and encounter_id=?
            order by sequence_number desc limit ? offset ?
            """, MAPPER, facility, encounter, size, (long) page * size);
    }
    public Map<String, Object> note(UUID facility, UUID encounter, UUID id) {
        return one("select * from medora.clinical_note where facility_id=? and encounter_id=? and id=?", facility, encounter, id);
    }
    public boolean hasDraft(UUID encounter) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
            select exists(select 1 from medora.clinical_note where encounter_id=? and status in ('DRAFT','AMENDMENT_DRAFT'))
            """, Boolean.class, encounter));
    }
    public Optional<Map<String, Object>> latestSigned(UUID encounter) {
        return jdbc.query("""
            select * from medora.clinical_note where encounter_id=? and status in ('SIGNED','AMENDED_SIGNED_VERSION')
            order by sequence_number desc limit 1
            """, MAPPER, encounter).stream().findFirst();
    }
    public Map<String, Object> newDraft(UUID facility, UUID encounter, UUID predecessor, String reason, NoteInput r) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
            insert into medora.clinical_note(id,facility_id,encounter_id,sequence_number,status,
                subjective,objective,assessment,plan,predecessor_id,amendment_reason)
            select ?,?,?,coalesce(max(sequence_number),0)+1,?,?,?,?,?,?,?
            from medora.clinical_note where encounter_id=?
            """, id, facility, encounter, predecessor == null ? "DRAFT" : "AMENDMENT_DRAFT",
            r.subjective(), r.objective(), r.assessment(), r.plan(), predecessor, reason, encounter);
        return note(facility, encounter, id);
    }
    public Map<String, Object> updateDraft(UUID facility, UUID encounter, UUID id, NoteInput r) {
        jdbc.update("""
            update medora.clinical_note set subjective=?,objective=?,assessment=?,plan=?,version=version+1 where id=?
            """, r.subjective(), r.objective(), r.assessment(), r.plan(), id);
        return note(facility, encounter, id);
    }
    public Map<String, Object> sign(UUID facility, UUID encounter, UUID id, UUID actor, boolean amendment) {
        jdbc.update("""
            update medora.clinical_note set status=?,signed_by=?,signed_at=current_timestamp,version=version+1 where id=?
            """, amendment ? "AMENDED_SIGNED_VERSION" : "SIGNED", actor, id);
        return note(facility, encounter, id);
    }
    public void abandon(UUID id) { jdbc.update("delete from medora.clinical_note where id=?", id); }
}
