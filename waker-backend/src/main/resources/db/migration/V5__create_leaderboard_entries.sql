CREATE TABLE leaderboard_entries (
    id                  UUID PRIMARY KEY,
    commitment_id       UUID         NOT NULL,
    user_display_name   VARCHAR(201) NOT NULL,
    commitment_name     VARCHAR(200) NOT NULL,
    missed_at           TIMESTAMPTZ  NOT NULL,
    CONSTRAINT leaderboard_entries_commitment_id_unique UNIQUE (commitment_id)
);

CREATE INDEX leaderboard_entries_missed_at_idx
    ON leaderboard_entries (missed_at DESC);
