CREATE TABLE penalty_events (
    id              UUID PRIMARY KEY,
    commitment_id   UUID         NOT NULL REFERENCES commitments (id),
    penalty_type    VARCHAR(40)  NOT NULL,
    outcome         VARCHAR(20)  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT penalty_events_commitment_id_unique UNIQUE (commitment_id),
    CONSTRAINT penalty_events_penalty_type_check
        CHECK (penalty_type IN ('EMAIL_TO_CONTACT', 'LEADERBOARD')),
    CONSTRAINT penalty_events_outcome_check
        CHECK (outcome IN ('PENDING', 'IN_PROGRESS', 'DISPATCHED', 'FAILED'))
);

-- Dispatcher scan (Story 3.5): PENDING rows by age
CREATE INDEX penalty_events_pending_created_at_idx
    ON penalty_events (created_at)
    WHERE outcome = 'PENDING';

-- Defense-in-depth; table owner bypasses REVOKE — triggers are the load-bearing enforcement (AD-7).
REVOKE DELETE ON penalty_events FROM PUBLIC;

CREATE OR REPLACE FUNCTION penalty_events_reject_delete()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'penalty_events is append-only: DELETE is forbidden';
END;
$$;

CREATE TRIGGER penalty_events_no_delete
    BEFORE DELETE ON penalty_events
    FOR EACH ROW
    EXECUTE FUNCTION penalty_events_reject_delete();

CREATE OR REPLACE FUNCTION penalty_events_enforce_outcome_update()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.id IS DISTINCT FROM OLD.id
       OR NEW.commitment_id IS DISTINCT FROM OLD.commitment_id
       OR NEW.penalty_type IS DISTINCT FROM OLD.penalty_type
       OR NEW.created_at IS DISTINCT FROM OLD.created_at THEN
        RAISE EXCEPTION 'penalty_events is append-only: only outcome may change';
    END IF;

    IF NOT (
        (OLD.outcome = 'PENDING' AND NEW.outcome = 'IN_PROGRESS')
        OR (OLD.outcome = 'IN_PROGRESS' AND NEW.outcome = 'DISPATCHED')
        OR (OLD.outcome = 'IN_PROGRESS' AND NEW.outcome = 'FAILED')
    ) THEN
        RAISE EXCEPTION 'penalty_events: illegal outcome transition % → %',
            OLD.outcome, NEW.outcome;
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER penalty_events_outcome_only
    BEFORE UPDATE ON penalty_events
    FOR EACH ROW
    EXECUTE FUNCTION penalty_events_enforce_outcome_update();
