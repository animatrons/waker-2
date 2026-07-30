CREATE TABLE commitments (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL REFERENCES users (id),
    name            VARCHAR(200)  NOT NULL,
    description     VARCHAR(1000),
    status          VARCHAR(20)   NOT NULL,
    notify_time     TIMESTAMPTZ   NOT NULL,
    deadline        TIMESTAMPTZ   NOT NULL,
    mission_config  JSONB         NOT NULL,
    penalty_config  JSONB         NOT NULL,
    created_at      TIMESTAMPTZ   NOT NULL,
    updated_at      TIMESTAMPTZ   NOT NULL,
    CONSTRAINT commitments_status_check
        CHECK (status IN ('PENDING', 'FULFILLED', 'MISSED', 'CANCELLED'))
);

CREATE INDEX commitments_user_id_status_idx ON commitments (user_id, status);
