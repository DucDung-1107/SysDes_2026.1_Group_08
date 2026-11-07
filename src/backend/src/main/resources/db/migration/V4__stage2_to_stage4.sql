-- Single clinic MVP. btree_gist enforces resource exclusion even under concurrent requests.
CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE medora.audit_event (
    id UUID PRIMARY KEY,
    actor_id UUID NOT NULL REFERENCES medora.user_account(id),
    facility_id UUID NOT NULL REFERENCES medora.facility(id),
    action VARCHAR(64) NOT NULL,
    target_type VARCHAR(40) NOT NULL,
    target_id UUID NOT NULL,
    related_id UUID,
    detail VARCHAR(500),
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX ix_audit_target ON medora.audit_event(facility_id, target_id, occurred_at);
CREATE TRIGGER audit_append_only BEFORE UPDATE OR DELETE ON medora.audit_event
FOR EACH ROW EXECUTE FUNCTION medora.reject_identity_audit_change();

CREATE TABLE medora.patient (
    id UUID PRIMARY KEY,
    facility_id UUID NOT NULL REFERENCES medora.facility(id),
    code VARCHAR(40) NOT NULL UNIQUE,
    full_name VARCHAR(160) NOT NULL,
    normalized_name VARCHAR(160) NOT NULL,
    date_of_birth DATE NOT NULL,
    sex VARCHAR(16) NOT NULL CHECK (sex IN ('MALE','FEMALE','OTHER','UNKNOWN')),
    phone VARCHAR(24), national_id VARCHAR(32), address VARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (id, facility_id)
);
CREATE INDEX ix_patient_name_dob ON medora.patient(facility_id, normalized_name, date_of_birth);
CREATE INDEX ix_patient_phone ON medora.patient(facility_id, phone);
CREATE UNIQUE INDEX uq_patient_national_id ON medora.patient(facility_id, national_id)
WHERE national_id IS NOT NULL;

CREATE TABLE medora.room (
    id UUID PRIMARY KEY, facility_id UUID NOT NULL REFERENCES medora.facility(id),
    code VARCHAR(32) NOT NULL, name VARCHAR(160) NOT NULL,
    room_type VARCHAR(40) NOT NULL, equipment VARCHAR(500) NOT NULL DEFAULT '',
    capacity INTEGER NOT NULL DEFAULT 1 CHECK (capacity = 1),
    enabled BOOLEAN NOT NULL DEFAULT TRUE, version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (id, facility_id), UNIQUE (facility_id, code)
);
CREATE TABLE medora.clinic_service (
    id UUID PRIMARY KEY, facility_id UUID NOT NULL REFERENCES medora.facility(id),
    code VARCHAR(32) NOT NULL, name VARCHAR(160) NOT NULL,
    duration_minutes INTEGER NOT NULL CHECK (duration_minutes BETWEEN 5 AND 240),
    buffer_minutes INTEGER NOT NULL CHECK (buffer_minutes BETWEEN 0 AND 60),
    enabled BOOLEAN NOT NULL DEFAULT TRUE, version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (id, facility_id), UNIQUE (facility_id, code)
);
CREATE TABLE medora.provider_service (
    facility_id UUID NOT NULL REFERENCES medora.facility(id),
    provider_id UUID NOT NULL REFERENCES medora.user_account(id),
    service_id UUID NOT NULL,
    PRIMARY KEY(facility_id, provider_id, service_id),
    FOREIGN KEY(service_id, facility_id) REFERENCES medora.clinic_service(id, facility_id)
);
CREATE TABLE medora.staff_schedule (
    id UUID PRIMARY KEY, facility_id UUID NOT NULL REFERENCES medora.facility(id),
    provider_id UUID NOT NULL REFERENCES medora.user_account(id),
    kind VARCHAR(16) NOT NULL CHECK (kind IN ('WORK','ABSENCE')),
    starts_at TIMESTAMPTZ NOT NULL, ends_at TIMESTAMPTZ NOT NULL,
    CHECK (starts_at < ends_at), UNIQUE (id, facility_id)
);
CREATE INDEX ix_schedule_provider ON medora.staff_schedule(provider_id, starts_at, ends_at);

CREATE TABLE medora.appointment (
    id UUID PRIMARY KEY, facility_id UUID NOT NULL REFERENCES medora.facility(id),
    patient_id UUID NOT NULL, provider_id UUID NOT NULL REFERENCES medora.user_account(id),
    service_id UUID NOT NULL, room_id UUID NOT NULL,
    starts_at TIMESTAMPTZ NOT NULL, ends_at TIMESTAMPTZ NOT NULL,
    reserved_until TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('BOOKED','CONFIRMED','CHECKED_IN','NO_SHOW','CANCELED')),
    request_key VARCHAR(100) NOT NULL, request_hash VARCHAR(64) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (starts_at < ends_at AND ends_at <= reserved_until),
    UNIQUE (id, facility_id), UNIQUE (facility_id, request_key),
    FOREIGN KEY(patient_id, facility_id) REFERENCES medora.patient(id, facility_id),
    FOREIGN KEY(service_id, facility_id) REFERENCES medora.clinic_service(id, facility_id),
    FOREIGN KEY(room_id, facility_id) REFERENCES medora.room(id, facility_id)
);
CREATE INDEX ix_appointment_list ON medora.appointment(facility_id, starts_at, id);
CREATE TABLE medora.resource_booking (
    id UUID PRIMARY KEY, appointment_id UUID NOT NULL REFERENCES medora.appointment(id),
    kind VARCHAR(16) NOT NULL CHECK (kind IN ('PROVIDER','ROOM')),
    resource_id UUID NOT NULL, starts_at TIMESTAMPTZ NOT NULL, ends_at TIMESTAMPTZ NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CHECK(starts_at < ends_at), UNIQUE(appointment_id, kind),
    EXCLUDE USING gist (kind WITH =, resource_id WITH =,
        tstzrange(starts_at, ends_at, '[)') WITH &&) WHERE (active)
);
CREATE TABLE medora.appointment_event (
    id UUID PRIMARY KEY, facility_id UUID NOT NULL REFERENCES medora.facility(id),
    appointment_id UUID NOT NULL REFERENCES medora.appointment(id),
    actor_id UUID NOT NULL REFERENCES medora.user_account(id),
    action VARCHAR(32) NOT NULL, reason VARCHAR(500),
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TRIGGER appointment_event_append_only BEFORE UPDATE OR DELETE ON medora.appointment_event
FOR EACH ROW EXECUTE FUNCTION medora.reject_identity_audit_change();

CREATE TABLE medora.encounter (
    id UUID PRIMARY KEY, facility_id UUID NOT NULL REFERENCES medora.facility(id),
    patient_id UUID NOT NULL, appointment_id UUID UNIQUE REFERENCES medora.appointment(id),
    doctor_id UUID NOT NULL REFERENCES medora.user_account(id),
    nurse_id UUID REFERENCES medora.user_account(id),
    status VARCHAR(24) NOT NULL DEFAULT 'WAITING' CHECK
        (status IN ('WAITING','IN_CONSULTATION','AWAITING_SERVICE','AWAITING_CLOSURE','COMPLETED')),
    queue_number BIGINT GENERATED ALWAYS AS IDENTITY UNIQUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ,
    summary VARCHAR(10000), instructions VARCHAR(10000), follow_up VARCHAR(2000),
    completed_by UUID REFERENCES medora.user_account(id),
    UNIQUE(id, facility_id),
    FOREIGN KEY(patient_id, facility_id) REFERENCES medora.patient(id, facility_id),
    CHECK ((status = 'COMPLETED') = (completed_at IS NOT NULL AND completed_by IS NOT NULL))
);
CREATE UNIQUE INDEX uq_active_encounter ON medora.encounter(facility_id, patient_id)
WHERE status <> 'COMPLETED';
CREATE INDEX ix_encounter_queue ON medora.encounter(facility_id, doctor_id, status, queue_number);

CREATE FUNCTION medora.protect_completed_encounter() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.status = 'COMPLETED' THEN
        RAISE EXCEPTION 'Completed encounters are immutable';
    END IF;
    RETURN CASE WHEN TG_OP = 'DELETE' THEN OLD ELSE NEW END;
END;
$$;
CREATE TRIGGER completed_encounter_immutable BEFORE UPDATE OR DELETE ON medora.encounter
FOR EACH ROW EXECUTE FUNCTION medora.protect_completed_encounter();

CREATE TABLE medora.patient_history (
    id UUID PRIMARY KEY, facility_id UUID NOT NULL REFERENCES medora.facility(id),
    patient_id UUID NOT NULL, encounter_id UUID NOT NULL,
    kind VARCHAR(16) NOT NULL CHECK (kind IN ('ALLERGY','HISTORY','MEDICATION')),
    content VARCHAR(4000) NOT NULL,
    recorded_by UUID NOT NULL REFERENCES medora.user_account(id),
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    supersedes_id UUID REFERENCES medora.patient_history(id), correction_reason VARCHAR(500),
    FOREIGN KEY(patient_id, facility_id) REFERENCES medora.patient(id, facility_id),
    FOREIGN KEY(encounter_id, facility_id) REFERENCES medora.encounter(id, facility_id),
    CHECK(supersedes_id IS NULL OR length(trim(correction_reason)) > 0)
);
CREATE UNIQUE INDEX uq_history_successor ON medora.patient_history(supersedes_id)
WHERE supersedes_id IS NOT NULL;

CREATE TABLE medora.vital_sign (
    id UUID PRIMARY KEY, facility_id UUID NOT NULL REFERENCES medora.facility(id),
    encounter_id UUID NOT NULL,
    temperature_c NUMERIC(4,1), pulse_bpm INTEGER, systolic_mmhg INTEGER,
    diastolic_mmhg INTEGER, respiratory_rate INTEGER, spo2_percent NUMERIC(5,2),
    weight_kg NUMERIC(6,2), height_cm NUMERIC(5,1), screening VARCHAR(4000),
    measured_at TIMESTAMPTZ NOT NULL, recorded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    recorded_by UUID NOT NULL REFERENCES medora.user_account(id),
    verified BOOLEAN NOT NULL, supersedes_id UUID REFERENCES medora.vital_sign(id),
    correction_reason VARCHAR(500),
    FOREIGN KEY(encounter_id, facility_id) REFERENCES medora.encounter(id, facility_id),
    CHECK(temperature_c IS NULL OR temperature_c BETWEEN 25 AND 45),
    CHECK(pulse_bpm IS NULL OR pulse_bpm BETWEEN 20 AND 300),
    CHECK(systolic_mmhg IS NULL OR systolic_mmhg BETWEEN 30 AND 300),
    CHECK(diastolic_mmhg IS NULL OR diastolic_mmhg BETWEEN 10 AND 200),
    CHECK(systolic_mmhg IS NULL OR diastolic_mmhg IS NULL OR systolic_mmhg > diastolic_mmhg),
    CHECK(respiratory_rate IS NULL OR respiratory_rate BETWEEN 1 AND 100),
    CHECK(spo2_percent IS NULL OR spo2_percent BETWEEN 0 AND 100),
    CHECK(weight_kg IS NULL OR weight_kg > 0), CHECK(height_cm IS NULL OR height_cm > 0),
    CHECK(supersedes_id IS NULL OR length(trim(correction_reason)) > 0)
);
CREATE UNIQUE INDEX uq_vitals_successor ON medora.vital_sign(supersedes_id) WHERE supersedes_id IS NOT NULL;

CREATE TABLE medora.diagnosis_catalog (
    code VARCHAR(32) PRIMARY KEY, name VARCHAR(200) NOT NULL, enabled BOOLEAN NOT NULL DEFAULT TRUE
);
-- Synthetic teaching catalog only; no claim of ICD or clinical validation.
INSERT INTO medora.diagnosis_catalog(code, name) VALUES ('DEMO-001','Synthetic demonstration diagnosis');
CREATE TABLE medora.encounter_diagnosis (
    id UUID PRIMARY KEY, facility_id UUID NOT NULL REFERENCES medora.facility(id),
    encounter_id UUID NOT NULL, code VARCHAR(32) NOT NULL REFERENCES medora.diagnosis_catalog(code),
    symptoms VARCHAR(4000) NOT NULL, recorded_by UUID NOT NULL REFERENCES medora.user_account(id),
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(encounter_id, facility_id) REFERENCES medora.encounter(id, facility_id)
);
CREATE TABLE medora.clinical_note (
    id UUID PRIMARY KEY, facility_id UUID NOT NULL REFERENCES medora.facility(id),
    encounter_id UUID NOT NULL, sequence_number INTEGER NOT NULL,
    status VARCHAR(24) NOT NULL CHECK (status IN ('DRAFT','SIGNED','AMENDMENT_DRAFT','AMENDED_SIGNED_VERSION')),
    subjective VARCHAR(10000) NOT NULL DEFAULT '', objective VARCHAR(10000) NOT NULL DEFAULT '',
    assessment VARCHAR(10000) NOT NULL DEFAULT '', plan VARCHAR(10000) NOT NULL DEFAULT '',
    predecessor_id UUID REFERENCES medora.clinical_note(id), amendment_reason VARCHAR(500),
    signed_by UUID REFERENCES medora.user_account(id), signed_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(encounter_id, sequence_number),
    FOREIGN KEY(encounter_id, facility_id) REFERENCES medora.encounter(id, facility_id),
    CHECK((status IN ('SIGNED','AMENDED_SIGNED_VERSION')) = (signed_at IS NOT NULL AND signed_by IS NOT NULL)),
    CHECK(predecessor_id IS NULL OR length(trim(amendment_reason)) > 0)
);
CREATE UNIQUE INDEX uq_note_open_draft ON medora.clinical_note(encounter_id)
WHERE status IN ('DRAFT','AMENDMENT_DRAFT');
CREATE UNIQUE INDEX uq_note_successor ON medora.clinical_note(predecessor_id) WHERE predecessor_id IS NOT NULL;

CREATE FUNCTION medora.protect_signed_note() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.status IN ('SIGNED','AMENDED_SIGNED_VERSION') THEN
        RAISE EXCEPTION 'Signed clinical note versions are immutable';
    END IF;
    RETURN CASE WHEN TG_OP = 'DELETE' THEN OLD ELSE NEW END;
END;
$$;
CREATE TRIGGER signed_note_immutable BEFORE UPDATE OR DELETE ON medora.clinical_note
FOR EACH ROW EXECUTE FUNCTION medora.protect_signed_note();
CREATE TRIGGER history_append_only BEFORE UPDATE OR DELETE ON medora.patient_history
FOR EACH ROW EXECUTE FUNCTION medora.reject_identity_audit_change();
CREATE TRIGGER vitals_append_only BEFORE UPDATE OR DELETE ON medora.vital_sign
FOR EACH ROW EXECUTE FUNCTION medora.reject_identity_audit_change();
CREATE TRIGGER diagnoses_append_only BEFORE UPDATE OR DELETE ON medora.encounter_diagnosis
FOR EACH ROW EXECUTE FUNCTION medora.reject_identity_audit_change();

INSERT INTO medora.permission(code, description) VALUES
('schedule.read','Read scoped clinic schedules'), ('schedule.manage','Configure clinic resources and schedules'),
('appointment.write','Manage appointments and arrivals'), ('queue.read','Read permitted queue'),
('clinical.read','Read assigned clinical encounters and patient history'),
('screening.write','Record screening for assigned encounters'),
('history.write','Record history for assigned encounters'),
('clinical.write','Document assigned clinical encounters');
INSERT INTO medora.role_permission(role_code, permission_code) VALUES
('SYSTEM_ADMINISTRATOR','schedule.manage'), ('SYSTEM_ADMINISTRATOR','schedule.read'),
('RECEPTIONIST','schedule.read'), ('RECEPTIONIST','appointment.write'), ('RECEPTIONIST','queue.read'),
('NURSE','schedule.read'), ('NURSE','queue.read'), ('NURSE','clinical.read'),
('NURSE','screening.write'), ('NURSE','history.write'),
('DOCTOR','schedule.read'), ('DOCTOR','queue.read'), ('DOCTOR','clinical.read'),
('DOCTOR','history.write'), ('DOCTOR','clinical.write');
