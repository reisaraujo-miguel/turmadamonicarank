CREATE TABLE characters (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    slug varchar(80) NOT NULL,
    name varchar(120) NOT NULL,
    full_name varchar(160),
    bio text,
    image_url text,
    image_alt varchar(200),
    image_credit varchar(200),
    source_url text,
    first_appearance_year smallint
        CONSTRAINT characters_first_appearance_year_check
        CHECK (first_appearance_year IS NULL OR first_appearance_year BETWEEN 1900 AND 2100),
    first_appearance_work varchar(200),
    status varchar(16) NOT NULL DEFAULT 'ACTIVE'
        CONSTRAINT characters_status_check CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT characters_slug_unique UNIQUE (slug),
    CONSTRAINT characters_slug_format CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$')
);

CREATE INDEX characters_name_trgm
    ON characters USING gin (lower(immutable_unaccent(name)) gin_trgm_ops);
CREATE INDEX characters_full_name_trgm
    ON characters USING gin (lower(immutable_unaccent(coalesce(full_name, ''))) gin_trgm_ops);
CREATE INDEX characters_status_idx ON characters (status);

CREATE TABLE character_aliases (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    character_id uuid NOT NULL REFERENCES characters (id) ON DELETE CASCADE,
    alias varchar(120) NOT NULL
);

CREATE UNIQUE INDEX character_aliases_unique_idx
    ON character_aliases (character_id, lower(alias));
CREATE INDEX character_aliases_alias_trgm
    ON character_aliases USING gin (lower(immutable_unaccent(alias)) gin_trgm_ops);
