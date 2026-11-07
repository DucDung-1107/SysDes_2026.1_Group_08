INSERT INTO medora.permission(code,description) VALUES
('invoice.read','Read scoped invoices'),('invoice.write','Create and issue invoices'),('payment.write','Record payments'),
('refund.request','Request and execute approved refunds'),('refund.approve','Approve refunds requested by others'),
('invoice.discount','Apply reasoned discounts'),('price.manage','Manage effective price lists'),
('report.read','Read scoped operational and financial reports'),('report.export','Export scoped reports'),
('audit.read','Read scoped immutable audit evidence'),('merge.request','Request patient identity merge'),
('merge.execute','Perform verified patient identity merge'),('notification.manage','Configure in-app notification templates'),
('notification.send','Queue in-app notifications'),('policy.manage','Manage scoped workflow policies'),
('discharge.approve','Approve optional outpatient discharge requests');
INSERT INTO medora.role_permission(role_code,permission_code) VALUES
('CASHIER','invoice.read'),('CASHIER','invoice.write'),('CASHIER','payment.write'),('CASHIER','report.read'),
('FINANCE','invoice.read'),('FINANCE','refund.request'),('FINANCE','refund.approve'),('FINANCE','invoice.discount'),
('FINANCE','report.read'),('FINANCE','report.export'),
('CLINIC_MANAGER','invoice.read'),('CLINIC_MANAGER','refund.approve'),('CLINIC_MANAGER','price.manage'),
('CLINIC_MANAGER','report.read'),('CLINIC_MANAGER','report.export'),('CLINIC_MANAGER','policy.manage'),
('CLINIC_MANAGER','discharge.approve'),('CLINIC_MANAGER','notification.manage'),
('SYSTEM_ADMINISTRATOR','audit.read'),('SYSTEM_ADMINISTRATOR','notification.manage'),
('AUDITOR','audit.read'),('AUDITOR','report.read'),('AUDITOR','report.export'),('AUDITOR','invoice.read'),
('RECEPTIONIST','merge.request'),('RECEPTIONIST','notification.send'),
('AUTHORIZED_REGISTRAR','merge.execute'),('AUTHORIZED_REGISTRAR','patient.admin.read'),
('DOCTOR','report.read'),('NURSE','report.read');
ALTER TABLE medora.patient ADD COLUMN merged_into_id UUID;
ALTER TABLE medora.patient ADD CONSTRAINT fk_patient_canonical FOREIGN KEY(merged_into_id,facility_id) REFERENCES medora.patient(id,facility_id);
ALTER TABLE medora.patient ADD CONSTRAINT ck_patient_merge_self CHECK(id<>merged_into_id);
CREATE INDEX ix_patient_canonical ON medora.patient(facility_id,merged_into_id);
CREATE TABLE medora.merge_request (
    id UUID PRIMARY KEY, facility_id UUID NOT NULL REFERENCES medora.facility(id), source_id UUID NOT NULL, target_id UUID NOT NULL,
    reason VARCHAR(2000) NOT NULL, requested_by UUID NOT NULL REFERENCES medora.user_account(id),
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING' CHECK(status IN ('PENDING','COMPLETED','REJECTED')),
    verification_reason VARCHAR(2000), resolved_by UUID REFERENCES medora.user_account(id), resolved_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0, created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    FOREIGN KEY(source_id,facility_id) REFERENCES medora.patient(id,facility_id),
    FOREIGN KEY(target_id,facility_id) REFERENCES medora.patient(id,facility_id), CHECK(source_id<>target_id)
);
CREATE TABLE medora.clinic_policy (
    facility_id UUID PRIMARY KEY REFERENCES medora.facility(id),
    auto_refund_limit NUMERIC(18,0) NOT NULL DEFAULT 0 CHECK(auto_refund_limit>=0),
    discharge_approval_required BOOLEAN NOT NULL DEFAULT false,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE TABLE medora.service_price (
    id UUID PRIMARY KEY, facility_id UUID NOT NULL REFERENCES medora.facility(id), service_id UUID NOT NULL,
    amount NUMERIC(18,0) NOT NULL CHECK(amount>0), currency VARCHAR(3) NOT NULL DEFAULT 'VND' CHECK(currency='VND'),
    effective_from TIMESTAMPTZ NOT NULL, effective_to TIMESTAMPTZ, created_by UUID NOT NULL REFERENCES medora.user_account(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    FOREIGN KEY(service_id,facility_id) REFERENCES medora.clinic_service(id,facility_id),
    CHECK(effective_to IS NULL OR effective_to>effective_from),
    EXCLUDE USING gist (service_id WITH =, tstzrange(effective_from,effective_to,'[)') WITH &&)
);
CREATE TRIGGER price_history_immutable BEFORE UPDATE OR DELETE ON medora.service_price
FOR EACH ROW EXECUTE FUNCTION medora.reject_identity_audit_change();
CREATE TABLE medora.invoice (
    id UUID PRIMARY KEY, facility_id UUID NOT NULL REFERENCES medora.facility(id), encounter_id UUID NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK(status IN ('DRAFT','ISSUED','PARTIALLY_PAID','PAID')),
    currency VARCHAR(3) NOT NULL DEFAULT 'VND' CHECK(currency='VND'), subtotal NUMERIC(18,0) NOT NULL CHECK(subtotal>0),
    discount NUMERIC(18,0) NOT NULL DEFAULT 0 CHECK(discount>=0 AND discount<subtotal), discount_reason VARCHAR(500),
    created_by UUID NOT NULL REFERENCES medora.user_account(id), created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    issued_by UUID REFERENCES medora.user_account(id), issued_at TIMESTAMPTZ, version BIGINT NOT NULL DEFAULT 0,
    FOREIGN KEY(encounter_id,facility_id) REFERENCES medora.encounter(id,facility_id), UNIQUE(id,facility_id)
);
CREATE TABLE medora.invoice_item (
    id UUID PRIMARY KEY, invoice_id UUID NOT NULL REFERENCES medora.invoice(id),
    service_id UUID NOT NULL REFERENCES medora.clinic_service(id), price_id UUID NOT NULL REFERENCES medora.service_price(id),
    source_type VARCHAR(20) NOT NULL CHECK(source_type IN ('CONSULTATION','PROCEDURE')),
    source_id UUID NOT NULL, description VARCHAR(160) NOT NULL, unit_price NUMERIC(18,0) NOT NULL CHECK(unit_price>0),
    quantity INTEGER NOT NULL DEFAULT 1 CHECK(quantity=1), UNIQUE(invoice_id,source_type,source_id)
);
CREATE TRIGGER invoice_items_immutable BEFORE UPDATE OR DELETE ON medora.invoice_item
FOR EACH ROW EXECUTE FUNCTION medora.reject_identity_audit_change();
CREATE TABLE medora.payment (
    id UUID PRIMARY KEY, facility_id UUID NOT NULL REFERENCES medora.facility(id), invoice_id UUID NOT NULL,
    amount NUMERIC(18,0) NOT NULL CHECK(amount>0), currency VARCHAR(3) NOT NULL CHECK(currency='VND'),
    method VARCHAR(20) NOT NULL CHECK(method IN ('CASH','BANK_TRANSFER')), receipt_number VARCHAR(50) NOT NULL UNIQUE,
    recorded_by UUID NOT NULL REFERENCES medora.user_account(id), recorded_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    FOREIGN KEY(invoice_id,facility_id) REFERENCES medora.invoice(id,facility_id), UNIQUE(id,facility_id)
);
CREATE TRIGGER payment_immutable BEFORE UPDATE OR DELETE ON medora.payment
FOR EACH ROW EXECUTE FUNCTION medora.reject_identity_audit_change();
CREATE TABLE medora.refund (
    id UUID PRIMARY KEY, facility_id UUID NOT NULL REFERENCES medora.facility(id), payment_id UUID NOT NULL,
    amount NUMERIC(18,0) NOT NULL CHECK(amount>0), reason VARCHAR(500) NOT NULL,
    status VARCHAR(24) NOT NULL CHECK(status IN ('PENDING_APPROVAL','APPROVED','REJECTED','COMPLETED')),
    requested_by UUID NOT NULL REFERENCES medora.user_account(id), requested_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    approved_by UUID REFERENCES medora.user_account(id), approved_at TIMESTAMPTZ,
    completed_by UUID REFERENCES medora.user_account(id), completed_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0, FOREIGN KEY(payment_id,facility_id) REFERENCES medora.payment(id,facility_id),
    CHECK(approved_by IS NULL OR approved_by<>requested_by),
    CHECK((status='COMPLETED')=(completed_at IS NOT NULL AND completed_by IS NOT NULL))
);
CREATE TABLE medora.refund_event (
    id UUID PRIMARY KEY, refund_id UUID NOT NULL REFERENCES medora.refund(id),
    actor_id UUID NOT NULL REFERENCES medora.user_account(id), action VARCHAR(24) NOT NULL, reason VARCHAR(500),
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp
);
CREATE TRIGGER refund_events_immutable BEFORE UPDATE OR DELETE ON medora.refund_event
FOR EACH ROW EXECUTE FUNCTION medora.reject_identity_audit_change();
CREATE FUNCTION medora.protect_completed_refund() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP='DELETE' OR OLD.status='COMPLETED' OR
       NEW.amount<>OLD.amount OR NEW.payment_id<>OLD.payment_id OR NEW.requested_by<>OLD.requested_by OR NEW.reason<>OLD.reason THEN
        RAISE EXCEPTION 'Refund financial evidence is immutable';
    END IF;
    RETURN NEW;
END; $$;
CREATE TRIGGER completed_refund_immutable BEFORE UPDATE OR DELETE ON medora.refund
FOR EACH ROW EXECUTE FUNCTION medora.protect_completed_refund();
CREATE TABLE medora.discharge_request (
    id UUID PRIMARY KEY, facility_id UUID NOT NULL REFERENCES medora.facility(id), encounter_id UUID NOT NULL,
    reason VARCHAR(2000) NOT NULL, requested_by UUID NOT NULL REFERENCES medora.user_account(id),
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING' CHECK(status IN ('PENDING','APPROVED','REJECTED')),
    resolved_by UUID REFERENCES medora.user_account(id), resolved_at TIMESTAMPTZ, decision_reason VARCHAR(2000),
    version BIGINT NOT NULL DEFAULT 0, created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    FOREIGN KEY(encounter_id,facility_id) REFERENCES medora.encounter(id,facility_id)
);
CREATE TABLE medora.notification_template (
    id UUID PRIMARY KEY, facility_id UUID NOT NULL REFERENCES medora.facility(id), code VARCHAR(40) NOT NULL,
    channel VARCHAR(16) NOT NULL DEFAULT 'IN_APP' CHECK(channel='IN_APP'), body VARCHAR(2000) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true, version BIGINT NOT NULL DEFAULT 0, UNIQUE(facility_id,code)
);
CREATE TABLE medora.notification (
    id UUID PRIMARY KEY, facility_id UUID NOT NULL REFERENCES medora.facility(id),
    template_id UUID NOT NULL REFERENCES medora.notification_template(id), appointment_id UUID NOT NULL REFERENCES medora.appointment(id),
    recipient_id UUID NOT NULL REFERENCES medora.user_account(id), body VARCHAR(2000) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING' CHECK(status IN ('PENDING','DELIVERED','FAILED')),
    attempts INTEGER NOT NULL DEFAULT 0, next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    delivered_at TIMESTAMPTZ, read_at TIMESTAMPTZ, last_error VARCHAR(80),
    created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp
);
CREATE INDEX ix_notification_pending ON medora.notification(status,next_attempt_at);
