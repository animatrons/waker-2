-- Users table for registration (Story 1.4 / FR-1).
-- Email uniqueness is enforced at the database level (not an application-only pre-check).

CREATE TABLE users (
    id              UUID PRIMARY KEY,
    email           VARCHAR(320)  NOT NULL,
    password_hash   VARCHAR(255)  NOT NULL,
    first_name      VARCHAR(100)  NOT NULL,
    last_name       VARCHAR(100)  NOT NULL,
    created_at      TIMESTAMPTZ   NOT NULL,
    updated_at      TIMESTAMPTZ   NOT NULL,
    CONSTRAINT users_email_key UNIQUE (email)
);
