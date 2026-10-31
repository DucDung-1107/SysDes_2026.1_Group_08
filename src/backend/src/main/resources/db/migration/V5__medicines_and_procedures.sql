INSERT INTO medora.app_role(code,name) VALUES
('TECHNICIAN','Technician / Service Staff'),('PHARMACIST','Pharmacist / Medicine Staff'),
('CASHIER','Cashier'),('FINANCE','Authorized Finance Staff'),('CLINIC_MANAGER','Clinic Manager'),
('AUTHORIZED_REGISTRAR','Authorized Registrar'),('AUDITOR','Auditor');
INSERT INTO medora.permission(code,description) VALUES
('catalog.manage','Manage scoped clinical catalogs and rule sources'),
('prescription.read','Read assigned medication work'),('prescription.dispense','Record assigned dispensing or administration'),
('prescription.override','Override explicitly overridable medication rules'),
('procedure.execute','Execute assigned procedures'),('attachment.read','Read scoped private attachments'),
('attachment.write','Upload or archive scoped attachments');
INSERT INTO medora.role_permission(role_code,permission_code) VALUES
('CLINIC_MANAGER','catalog.manage'),('SYSTEM_ADMINISTRATOR','catalog.manage'),
('CLINIC_MANAGER','schedule.manage'),('CLINIC_MANAGER','schedule.read'),
('TECHNICIAN','schedule.read'),('TECHNICIAN','procedure.execute'),('TECHNICIAN','attachment.read'),('TECHNICIAN','attachment.write'),
('PHARMACIST','prescription.read'),('PHARMACIST','prescription.dispense'),
('DOCTOR','prescription.override'),('DOCTOR','attachment.read'),('DOCTOR','attachment.write'),
('NURSE','attachment.read'),('NURSE','attachment.write');

CREATE TABLE medora.request_result (
    facility_id UUID NOT NULL REFERENCES medora.facility(id), actor_id UUID NOT NULL REFERENCES medora.user_account(id),
    kind VARCHAR(40) NOT NULL, request_key VARCHAR(100) NOT NULL, request_hash VARCHAR(64) NOT NULL,
    response TEXT NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    PRIMARY KEY(facility_id,actor_id,kind,request_key)
);
CREATE TRIGGER request_result_immutable BEFORE UPDATE OR DELETE ON medora.request_result
FOR EACH ROW EXECUTE FUNCTION medora.reject_identity_audit_change();

CREATE TABLE medora.medication (
    id UUID PRIMARY KEY, facility_id UUID NOT NULL REFERENCES medora.facility(id),
    code VARCHAR(40) NOT NULL, name VARCHAR(160) NOT NULL, ingredient_code VARCHAR(80) NOT NULL,
    unit VARCHAR(40) NOT NULL, enabled BOOLEAN NOT NULL DEFAULT true, version BIGINT NOT NULL DEFAULT 0,
    UNIQUE(facility_id,code), UNIQUE(id,facility_id)
);
CREATE TABLE medora.medication_policy (
    facility_id UUID PRIMARY KEY REFERENCES medora.facility(id),
    source VARCHAR(500) NOT NULL, data_version VARCHAR(80) NOT NULL,
    enabled BOOLEAN NOT NULL, version BIGINT NOT NULL DEFAULT 0,
    approved_by UUID NOT NULL REFERENCES medora.user_account(id), approved_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp
);
CREATE TABLE medora.medication_rule (
    id UUID PRIMARY KEY, facility_id UUID NOT NULL REFERENCES medora.facility(id),
    medication_id UUID NOT NULL, other_medication_id UUID,
    kind VARCHAR(24) NOT NULL CHECK(kind IN ('INTERACTION','CONTRAINDICATION')),
    condition_code VARCHAR(80), severity VARCHAR(8) NOT NULL CHECK(severity IN ('WARN','BLOCK')),
    overridable BOOLEAN NOT NULL, message VARCHAR(500) NOT NULL, source VARCHAR(500) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true, version BIGINT NOT NULL DEFAULT 0,
    FOREIGN KEY(medication_id,facility_id) REFERENCES medora.medication(id,facility_id),
    FOREIGN KEY(other_medication_id,facility_id) REFERENCES medora.medication(id,facility_id),
    CHECK((kind='INTERACTION' AND other_medication_id IS NOT NULL) OR (kind='CONTRAINDICATION' AND condition_code IS NOT NULL))
);
CREATE TABLE medora.medication_profile (
    patient_id UUID PRIMARY KEY, facility_id UUID NOT NULL REFERENCES medora.facility(id),
    allergy_status VARCHAR(16) NOT NULL CHECK(allergy_status IN ('UNKNOWN','KNOWN_NONE','RECORDED')),
    allergy_codes VARCHAR(4000) NOT NULL, condition_codes VARCHAR(4000) NOT NULL, current_medication_ids VARCHAR(4000) NOT NULL,
    recorded_by UUID NOT NULL REFERENCES medora.user_account(id), recorded_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    version BIGINT NOT NULL DEFAULT 0,
    FOREIGN KEY(patient_id,facility_id) REFERENCES medora.patient(id,facility_id)
);
CREATE TABLE medora.prescription (
    id UUID PRIMARY KEY, facility_id UUID NOT NULL REFERENCES medora.facility(id), encounter_id UUID NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT' CHECK(status IN ('DRAFT','ISSUED','CANCELED','SUPERSEDED')),
    predecessor_id UUID UNIQUE REFERENCES medora.prescription(id), reason VARCHAR(500),
    pharmacist_id UUID REFERENCES medora.user_account(id), issued_by UUID REFERENCES medora.user_account(id), issued_at TIMESTAMPTZ,
    canceled_by UUID REFERENCES medora.user_account(id), canceled_at TIMESTAMPTZ,
    safety_source VARCHAR(500), safety_version VARCHAR(80), version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    FOREIGN KEY(encounter_id,facility_id) REFERENCES medora.encounter(id,facility_id), UNIQUE(id,facility_id),
    CHECK(status='DRAFT' OR (issued_at IS NOT NULL AND issued_by IS NOT NULL))
);
CREATE TABLE medora.prescription_item (
    id UUID PRIMARY KEY, prescription_id UUID NOT NULL REFERENCES medora.prescription(id),
    medication_id UUID NOT NULL REFERENCES medora.medication(id), medication_name VARCHAR(160) NOT NULL,
    ingredient_code VARCHAR(80) NOT NULL, unit VARCHAR(40) NOT NULL,
    dose VARCHAR(160) NOT NULL, route VARCHAR(80) NOT NULL, frequency VARCHAR(160) NOT NULL,
    duration_days INTEGER NOT NULL CHECK(duration_days BETWEEN 1 AND 365),
    quantity NUMERIC(12,3) NOT NULL CHECK(quantity>0), instructions VARCHAR(2000) NOT NULL,
    UNIQUE(prescription_id,medication_id)
);
CREATE TABLE medora.medication_event (
    id UUID PRIMARY KEY, facility_id UUID NOT NULL REFERENCES medora.facility(id),
    prescription_id UUID NOT NULL, item_id UUID NOT NULL REFERENCES medora.prescription_item(id),
    kind VARCHAR(16) NOT NULL CHECK(kind IN ('DISPENSED','ADMINISTERED')),
    quantity NUMERIC(12,3) NOT NULL CHECK(quantity>0), performed_by UUID NOT NULL REFERENCES medora.user_account(id),
    performed_at TIMESTAMPTZ NOT NULL, recorded_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    FOREIGN KEY(prescription_id,facility_id) REFERENCES medora.prescription(id,facility_id)
);
CREATE TRIGGER medication_event_immutable BEFORE UPDATE OR DELETE ON medora.medication_event
FOR EACH ROW EXECUTE FUNCTION medora.reject_identity_audit_change();
CREATE FUNCTION medora.protect_prescription_items() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE parent_id UUID;
BEGIN
    parent_id := CASE WHEN TG_OP='DELETE' THEN OLD.prescription_id ELSE NEW.prescription_id END;
    IF EXISTS(select 1 from medora.prescription where id=parent_id and status<>'DRAFT') THEN
        RAISE EXCEPTION 'Issued prescription items are immutable';
    END IF;
    RETURN CASE WHEN TG_OP='DELETE' THEN OLD ELSE NEW END;
END; $$;
CREATE TRIGGER prescription_items_immutable BEFORE INSERT OR UPDATE OR DELETE ON medora.prescription_item
FOR EACH ROW EXECUTE FUNCTION medora.protect_prescription_items();

ALTER TABLE medora.clinic_service ADD COLUMN required_room_type VARCHAR(40);
ALTER TABLE medora.clinic_service ADD COLUMN required_equipment VARCHAR(500) NOT NULL DEFAULT '';
ALTER TABLE medora.encounter ADD COLUMN consultation_service_id UUID REFERENCES medora.clinic_service(id);
CREATE TABLE medora.procedure_order (
    id UUID PRIMARY KEY, facility_id UUID NOT NULL REFERENCES medora.facility(id), encounter_id UUID NOT NULL,
    service_id UUID NOT NULL, service_name VARCHAR(160) NOT NULL, reason VARCHAR(2000) NOT NULL,
    priority VARCHAR(16) NOT NULL CHECK(priority IN ('ROUTINE','URGENT')), preparation VARCHAR(2000) NOT NULL,
    required BOOLEAN NOT NULL, kind VARCHAR(16) NOT NULL CHECK(kind IN ('PROCEDURE','LAB','IMAGING')),
    status VARCHAR(24) NOT NULL DEFAULT 'PENDING_ALLOCATION' CHECK(status IN
        ('CREATED','PENDING_ALLOCATION','SCHEDULED','DELAYED','IN_PROGRESS','COMPLETED','NOT_PERFORMED','CANCELED')),
    staff_id UUID REFERENCES medora.user_account(id), room_id UUID REFERENCES medora.room(id),
    starts_at TIMESTAMPTZ, ends_at TIMESTAMPTZ, reserved_until TIMESTAMPTZ,
    started_at TIMESTAMPTZ, completed_at TIMESTAMPTZ, completed_by UUID REFERENCES medora.user_account(id),
    result TEXT, resolution_reason VARCHAR(2000), reviewed_by UUID REFERENCES medora.user_account(id), reviewed_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0, created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    FOREIGN KEY(encounter_id,facility_id) REFERENCES medora.encounter(id,facility_id),
    FOREIGN KEY(service_id,facility_id) REFERENCES medora.clinic_service(id,facility_id), UNIQUE(id,facility_id)
);
CREATE TABLE medora.procedure_event (
    id UUID PRIMARY KEY, order_id UUID NOT NULL REFERENCES medora.procedure_order(id),
    actor_id UUID NOT NULL REFERENCES medora.user_account(id), action VARCHAR(40) NOT NULL,
    reason VARCHAR(2000), occurred_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp
);
CREATE TRIGGER procedure_event_immutable BEFORE UPDATE OR DELETE ON medora.procedure_event
FOR EACH ROW EXECUTE FUNCTION medora.reject_identity_audit_change();
ALTER TABLE medora.resource_booking ALTER COLUMN appointment_id DROP NOT NULL;
ALTER TABLE medora.resource_booking ADD COLUMN procedure_order_id UUID REFERENCES medora.procedure_order(id);
ALTER TABLE medora.resource_booking ADD CONSTRAINT ck_booking_owner CHECK(num_nonnulls(appointment_id,procedure_order_id)=1);
ALTER TABLE medora.resource_booking ADD CONSTRAINT uq_order_booking UNIQUE(procedure_order_id,kind);
CREATE TABLE medora.attachment (
    id UUID PRIMARY KEY, facility_id UUID NOT NULL REFERENCES medora.facility(id), encounter_id UUID NOT NULL,
    order_id UUID REFERENCES medora.procedure_order(id), filename VARCHAR(200) NOT NULL,
    media_type VARCHAR(80) NOT NULL, byte_size BIGINT NOT NULL CHECK(byte_size BETWEEN 1 AND 10485760),
    sha256 VARCHAR(64) NOT NULL, storage_key VARCHAR(200) NOT NULL UNIQUE,
    uploaded_by UUID NOT NULL REFERENCES medora.user_account(id), uploaded_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    archived BOOLEAN NOT NULL DEFAULT false, archive_reason VARCHAR(500), version BIGINT NOT NULL DEFAULT 0,
    FOREIGN KEY(encounter_id,facility_id) REFERENCES medora.encounter(id,facility_id)
);
