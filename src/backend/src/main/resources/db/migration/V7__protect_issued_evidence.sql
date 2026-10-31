ALTER TABLE medora.prescription ADD COLUMN cancel_reason VARCHAR(500);
CREATE FUNCTION medora.protect_issued_prescription() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.status<>'DRAFT' THEN
        IF TG_OP='DELETE' OR OLD.status<>'ISSUED' OR NEW.status NOT IN ('CANCELED','SUPERSEDED') OR
            (to_jsonb(NEW)-ARRAY['status','canceled_at','canceled_by','cancel_reason','version'])
            IS DISTINCT FROM (to_jsonb(OLD)-ARRAY['status','canceled_at','canceled_by','cancel_reason','version']) THEN
            RAISE EXCEPTION 'Issued prescription evidence is immutable';
        END IF;
    END IF;
    RETURN CASE WHEN TG_OP='DELETE' THEN OLD ELSE NEW END;
END; $$;
CREATE TRIGGER issued_prescription_immutable BEFORE UPDATE OR DELETE ON medora.prescription
FOR EACH ROW EXECUTE FUNCTION medora.protect_issued_prescription();
CREATE OR REPLACE FUNCTION medora.protect_prescription_items() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP<>'INSERT' AND EXISTS(select 1 from medora.prescription where id=OLD.prescription_id and status<>'DRAFT') THEN
        RAISE EXCEPTION 'Issued prescription items are immutable';
    END IF;
    IF TG_OP<>'DELETE' AND EXISTS(select 1 from medora.prescription where id=NEW.prescription_id and status<>'DRAFT') THEN
        RAISE EXCEPTION 'Issued prescription items are immutable';
    END IF;
    RETURN CASE WHEN TG_OP='DELETE' THEN OLD ELSE NEW END;
END; $$;
CREATE FUNCTION medora.protect_procedure_result() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.status IN ('COMPLETED','CANCELED','NOT_PERFORMED') AND
        (TG_OP='DELETE' OR (to_jsonb(NEW)-ARRAY['reviewed_by','reviewed_at','version'])
            IS DISTINCT FROM (to_jsonb(OLD)-ARRAY['reviewed_by','reviewed_at','version'])) THEN
        RAISE EXCEPTION 'Terminal procedure evidence is immutable';
    END IF;
    RETURN CASE WHEN TG_OP='DELETE' THEN OLD ELSE NEW END;
END; $$;
CREATE TRIGGER procedure_result_immutable BEFORE UPDATE OR DELETE ON medora.procedure_order
FOR EACH ROW EXECUTE FUNCTION medora.protect_procedure_result();
CREATE FUNCTION medora.protect_issued_invoice() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.status<>'DRAFT' AND (TG_OP='DELETE' OR
        (to_jsonb(NEW)-ARRAY['status','version']) IS DISTINCT FROM (to_jsonb(OLD)-ARRAY['status','version'])) THEN
        RAISE EXCEPTION 'Issued invoice evidence is immutable';
    END IF;
    RETURN CASE WHEN TG_OP='DELETE' THEN OLD ELSE NEW END;
END; $$;
CREATE TRIGGER issued_invoice_immutable BEFORE UPDATE OR DELETE ON medora.invoice
FOR EACH ROW EXECUTE FUNCTION medora.protect_issued_invoice();
CREATE FUNCTION medora.require_draft_invoice() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NOT EXISTS(select 1 from medora.invoice where id=NEW.invoice_id and status='DRAFT') THEN
        RAISE EXCEPTION 'Invoice items require a draft invoice';
    END IF;
    RETURN NEW;
END; $$;
CREATE TRIGGER invoice_item_draft_only BEFORE INSERT ON medora.invoice_item
FOR EACH ROW EXECUTE FUNCTION medora.require_draft_invoice();
DROP TRIGGER price_history_immutable ON medora.service_price;
CREATE FUNCTION medora.protect_service_price() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP='DELETE' OR OLD.effective_to IS NOT NULL OR NEW.effective_to IS NULL
       OR NEW.effective_to<=greatest(current_timestamp,OLD.effective_from)
       OR (to_jsonb(NEW)-'effective_to') IS DISTINCT FROM (to_jsonb(OLD)-'effective_to') THEN
        RAISE EXCEPTION 'Price history is immutable; only a future end of an open period is allowed';
    END IF;
    RETURN NEW;
END; $$;
CREATE TRIGGER price_history_immutable BEFORE UPDATE OR DELETE ON medora.service_price
FOR EACH ROW EXECUTE FUNCTION medora.protect_service_price();
ALTER TABLE medora.diagnosis_catalog ADD COLUMN facility_id UUID REFERENCES medora.facility(id);
CREATE FUNCTION medora.check_diagnosis_scope() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NOT EXISTS(select 1 from medora.diagnosis_catalog where code=NEW.code and (facility_id IS NULL OR facility_id=NEW.facility_id)) THEN
        RAISE EXCEPTION 'Diagnosis code is outside facility scope';
    END IF;
    RETURN NEW;
END; $$;
CREATE TRIGGER diagnosis_scope BEFORE INSERT ON medora.encounter_diagnosis
FOR EACH ROW EXECUTE FUNCTION medora.check_diagnosis_scope();
