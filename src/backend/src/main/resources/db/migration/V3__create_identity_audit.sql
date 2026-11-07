CREATE TABLE medora.identity_audit_event (
    id UUID PRIMARY KEY,
    actor_account_id UUID NOT NULL REFERENCES medora.user_account(id),
    target_account_id UUID NOT NULL REFERENCES medora.user_account(id),
    facility_id UUID NOT NULL REFERENCES medora.facility(id),
    action VARCHAR(64) NOT NULL,
    detail VARCHAR(500) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX ix_identity_audit_target_time
    ON medora.identity_audit_event(target_account_id, occurred_at);

-- Prevent ordinary row updates/deletes, including accidental application changes.
-- Database owners/superusers can bypass controls; this is not tamper-proof storage.
CREATE FUNCTION medora.reject_identity_audit_change()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Identity audit events are append-only';
END;
$$;

CREATE TRIGGER identity_audit_append_only
BEFORE UPDATE OR DELETE ON medora.identity_audit_event
FOR EACH ROW EXECUTE FUNCTION medora.reject_identity_audit_change();
