CREATE TABLE ballots (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ballots_user_unique UNIQUE (user_id)
);

CREATE TABLE ballot_entries (
    ballot_id uuid NOT NULL REFERENCES ballots (id) ON DELETE CASCADE,
    character_id uuid NOT NULL REFERENCES characters (id) ON DELETE RESTRICT,
    position smallint NOT NULL,
    CONSTRAINT ballot_entries_pk PRIMARY KEY (ballot_id, position),
    CONSTRAINT ballot_entries_position_check CHECK (position BETWEEN 1 AND 10),
    CONSTRAINT ballot_entries_character_unique UNIQUE (ballot_id, character_id)
);

CREATE INDEX ballot_entries_character_idx ON ballot_entries (character_id);
