DROP TRIGGER IF EXISTS trg_insert_metadata_vng ON versioned_named_graph^;
DROP FUNCTION IF EXISTS trg_fn_insert_metadata_vng^;
DROP FUNCTION IF EXISTS version_named_graph(varchar, varchar, integer)^;

CREATE TABLE IF NOT EXISTS resource_or_literal
(
    id_resource_or_literal integer PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    name                   text,
    type                   varchar(255),
    -- Native (pre-parsed) representations of the literal. They are populated
    -- automatically from (name, type) by trg_resource_or_literal_native_type so
    -- the query side never has to cast the textual `name` at query time.
    numeric_value          numeric,
    datetime_value         timestamptz,
    boolean_value          boolean
)^;

-- Make the native columns available on databases created before this change.
ALTER TABLE resource_or_literal ADD COLUMN IF NOT EXISTS numeric_value  numeric^;
ALTER TABLE resource_or_literal ADD COLUMN IF NOT EXISTS datetime_value timestamptz^;
ALTER TABLE resource_or_literal ADD COLUMN IF NOT EXISTS boolean_value  boolean^;

-- Exception-safe casts: an ill-formed lexical form yields NULL instead of
-- aborting the (batched) insert.
CREATE OR REPLACE FUNCTION try_cast_numeric(v text)
    RETURNS numeric
    LANGUAGE plpgsql
    IMMUTABLE
AS
$$
BEGIN
    RETURN v::numeric;
EXCEPTION
    WHEN others THEN RETURN NULL;
END;
$$^;

CREATE OR REPLACE FUNCTION try_cast_timestamptz(v text)
    RETURNS timestamptz
    LANGUAGE plpgsql
    IMMUTABLE
AS
$$
BEGIN
    RETURN v::timestamptz;
EXCEPTION
    WHEN others THEN RETURN NULL;
END;
$$^;

CREATE OR REPLACE FUNCTION try_cast_boolean(v text)
    RETURNS boolean
    LANGUAGE plpgsql
    IMMUTABLE
AS
$$
BEGIN
    RETURN v::boolean;
EXCEPTION
    WHEN others THEN RETURN NULL;
END;
$$^;

-- Populate the native columns from the RDF datatype URI. Centralised in a
-- trigger so every insert path (catalog, version_named_graph, save) benefits.
CREATE OR REPLACE FUNCTION trg_fn_resource_or_literal_native_type()
    RETURNS trigger
    LANGUAGE plpgsql
AS
$$
BEGIN
    NEW.numeric_value := NULL;
    NEW.datetime_value := NULL;
    NEW.boolean_value := NULL;

    IF NEW.type IS NOT NULL THEN
        CASE
            WHEN NEW.type IN (
                'http://www.w3.org/2001/XMLSchema#integer',
                'http://www.w3.org/2001/XMLSchema#decimal',
                'http://www.w3.org/2001/XMLSchema#double',
                'http://www.w3.org/2001/XMLSchema#float',
                'http://www.w3.org/2001/XMLSchema#long',
                'http://www.w3.org/2001/XMLSchema#int',
                'http://www.w3.org/2001/XMLSchema#short',
                'http://www.w3.org/2001/XMLSchema#byte',
                'http://www.w3.org/2001/XMLSchema#nonNegativeInteger',
                'http://www.w3.org/2001/XMLSchema#nonPositiveInteger',
                'http://www.w3.org/2001/XMLSchema#negativeInteger',
                'http://www.w3.org/2001/XMLSchema#positiveInteger',
                'http://www.w3.org/2001/XMLSchema#unsignedLong',
                'http://www.w3.org/2001/XMLSchema#unsignedInt',
                'http://www.w3.org/2001/XMLSchema#unsignedShort',
                'http://www.w3.org/2001/XMLSchema#unsignedByte'
            ) THEN
                NEW.numeric_value := try_cast_numeric(NEW.name);
            WHEN NEW.type IN (
                'http://www.w3.org/2001/XMLSchema#dateTime',
                'http://www.w3.org/2001/XMLSchema#dateTimeStamp',
                'http://www.w3.org/2001/XMLSchema#date'
            ) THEN
                NEW.datetime_value := try_cast_timestamptz(NEW.name);
            WHEN NEW.type = 'http://www.w3.org/2001/XMLSchema#boolean' THEN
                NEW.boolean_value := try_cast_boolean(NEW.name);
            ELSE
                NULL;
        END CASE;
    END IF;

    RETURN NEW;
END;
$$^;

CREATE OR REPLACE TRIGGER trg_resource_or_literal_native_type
    BEFORE INSERT OR UPDATE ON resource_or_literal
    FOR EACH ROW
EXECUTE FUNCTION trg_fn_resource_or_literal_native_type()^;

CREATE TABLE IF NOT EXISTS versioned_named_graph
(
    id_versioned_named_graph integer PRIMARY KEY REFERENCES resource_or_literal (id_resource_or_literal),
    id_named_graph           integer REFERENCES resource_or_literal (id_resource_or_literal),
    index_version            integer
)^;


CREATE UNIQUE INDEX IF NOT EXISTS resource_or_literal_idx ON resource_or_literal (sha512(name::bytea), type) NULLS NOT DISTINCT^;

CREATE TABLE IF NOT EXISTS versioned_quad
(
    id_object      integer REFERENCES resource_or_literal (id_resource_or_literal),
    id_predicate   integer REFERENCES resource_or_literal (id_resource_or_literal),
    id_subject     integer REFERENCES resource_or_literal (id_resource_or_literal),
    id_named_graph integer REFERENCES resource_or_literal (id_resource_or_literal),
    validity       bit varying,
    PRIMARY KEY (id_object, id_predicate, id_subject, id_named_graph)
)^;

CREATE INDEX IF NOT EXISTS versioned_quad_ng_s_p_o ON versioned_quad (id_named_graph, id_subject, id_predicate, id_object)^;
CREATE INDEX IF NOT EXISTS versioned_quad_ng_s_o_p ON versioned_quad (id_named_graph, id_subject, id_object, id_predicate)^;
CREATE INDEX IF NOT EXISTS versioned_quad_ng_p_o_s ON versioned_quad (id_named_graph, id_predicate, id_object, id_subject)^;
CREATE INDEX IF NOT EXISTS versioned_quad_ng_p_s_o ON versioned_quad (id_named_graph, id_predicate, id_subject, id_object)^;
CREATE INDEX IF NOT EXISTS versioned_quad_ng_o_p_s ON versioned_quad (id_named_graph, id_object, id_predicate, id_subject)^;
CREATE INDEX IF NOT EXISTS versioned_quad_ng_o_s_p ON versioned_quad (id_named_graph, id_object, id_subject, id_predicate)^;


CREATE TABLE IF NOT EXISTS flat_model_quad
(
    id_record      integer PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    subject        text,
    subject_type   varchar,
    predicate      text,
    predicate_type varchar,
    object         text,
    object_type    varchar,
    named_graph    text,
    version        integer
)^;

CREATE TABLE IF NOT EXISTS flat_model_triple
(
    id_record      integer PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    subject        text,
    subject_type   varchar,
    predicate      text,
    predicate_type varchar,
    object         text,
    object_type    varchar
)^;

CREATE TABLE IF NOT EXISTS versioned_quad_flat
(
    id_object      integer REFERENCES resource_or_literal (id_resource_or_literal),
    id_predicate   integer REFERENCES resource_or_literal (id_resource_or_literal),
    id_subject     integer REFERENCES resource_or_literal (id_resource_or_literal),
    id_versioned_named_graph integer REFERENCES resource_or_literal (id_resource_or_literal),
    PRIMARY KEY (id_object, id_predicate, id_subject, id_versioned_named_graph)
)^;

CREATE INDEX IF NOT EXISTS versioned_quad_flat_vng_s_p_o ON versioned_quad_flat (id_versioned_named_graph, id_subject, id_predicate, id_object)^;
CREATE INDEX IF NOT EXISTS versioned_quad_flat_vng_s_o_p ON versioned_quad_flat (id_versioned_named_graph, id_subject, id_object, id_predicate)^;
CREATE INDEX IF NOT EXISTS versioned_quad_flat_vng_p_o_s ON versioned_quad_flat (id_versioned_named_graph, id_predicate, id_object, id_subject)^;
CREATE INDEX IF NOT EXISTS versioned_quad_flat_vng_p_s_o ON versioned_quad_flat (id_versioned_named_graph, id_predicate, id_subject, id_object)^;
CREATE INDEX IF NOT EXISTS versioned_quad_flat_vng_o_p_s ON versioned_quad_flat (id_versioned_named_graph, id_object, id_predicate, id_subject)^;
CREATE INDEX IF NOT EXISTS versioned_quad_flat_vng_o_s_p ON versioned_quad_flat (id_versioned_named_graph, id_object, id_subject, id_predicate)^;

CREATE TABLE IF NOT EXISTS version
(
    index_version          integer PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    message                varchar(255),
    transaction_time_start timestamptz default current_timestamp,
    transaction_time_end   timestamptz default NULL
)^;

CREATE TABLE IF NOT EXISTS metadata
(
    id_object    integer REFERENCES resource_or_literal (id_resource_or_literal),
    id_predicate integer REFERENCES resource_or_literal (id_resource_or_literal),
    id_subject   integer REFERENCES resource_or_literal (id_resource_or_literal),
    PRIMARY KEY (id_object, id_predicate, id_subject)
)^;

CREATE OR REPLACE FUNCTION version_named_graph(
    named_graph varchar,
    filename varchar,
    version integer
)
    RETURNS void
    LANGUAGE plpgsql
AS
$$
DECLARE
    ng_id integer;
    v_id integer;
    vng_id integer;
BEGIN
    INSERT INTO resource_or_literal
        VALUES (DEFAULT, named_graph, NULL)
        ON CONFLICT (sha512(name::bytea), type) DO UPDATE SET type = EXCLUDED.type
        RETURNING id_resource_or_literal INTO ng_id;

    INSERT INTO resource_or_literal
        VALUES (DEFAULT, 'https://github.com/VCityTeam/ConVer-G/Version#' || filename, NULL)
        ON CONFLICT (sha512(name::bytea), type) DO UPDATE SET type = EXCLUDED.type
        RETURNING id_resource_or_literal INTO v_id;

    INSERT INTO resource_or_literal
        VALUES (DEFAULT, 'https://github.com/VCityTeam/ConVer-G/Versioned-Named-Graph#' ||
                        encode(sha512((named_graph || filename)::bytea), 'hex'), NULL)
        ON CONFLICT (sha512(name::bytea), type) DO UPDATE SET type = EXCLUDED.type
        RETURNING id_resource_or_literal INTO vng_id;

    INSERT INTO versioned_named_graph
        VALUES (vng_id, ng_id, version)
        ON CONFLICT (id_versioned_named_graph) DO UPDATE SET id_named_graph = EXCLUDED.id_named_graph;
END;
$$^;

CREATE OR REPLACE FUNCTION trg_fn_insert_metadata_vng()
    RETURNS trigger
    LANGUAGE plpgsql
AS $$
DECLARE
    v_id integer;
    pred_spec integer;
    pred_loc integer;
    pred_atloc integer;
    pred_type integer;
    prov_entity integer;
BEGIN
    SELECT rl.id_resource_or_literal INTO v_id FROM resource_or_literal rl JOIN version v ON rl.name = v.message AND rl.type IS NULL WHERE v.index_version = NEW.index_version;
    SELECT id_resource_or_literal INTO pred_spec FROM resource_or_literal WHERE name = 'http://www.w3.org/ns/prov#specializationOf';
    SELECT id_resource_or_literal INTO pred_atloc FROM resource_or_literal WHERE name = 'http://www.w3.org/ns/prov#atLocation';
    SELECT id_resource_or_literal INTO pred_loc FROM resource_or_literal WHERE name = 'http://www.w3.org/ns/prov#Location';
    SELECT id_resource_or_literal INTO pred_type FROM resource_or_literal WHERE name = 'http://www.w3.org/1999/02/22-rdf-syntax-ns#type';
    SELECT id_resource_or_literal INTO prov_entity FROM resource_or_literal WHERE name = 'http://www.w3.org/ns/prov#Entity';

    INSERT INTO metadata (id_subject, id_predicate, id_object)
    VALUES
        (NEW.id_versioned_named_graph, pred_spec, NEW.id_named_graph),
        (NEW.id_versioned_named_graph, pred_atloc, v_id),
        (v_id, pred_type, pred_loc),
        (NEW.id_versioned_named_graph, pred_type, prov_entity),
        (NEW.id_named_graph, pred_type, prov_entity)
    ON CONFLICT ON CONSTRAINT metadata_pkey
        DO UPDATE SET id_subject = EXCLUDED.id_subject;

    RETURN NEW;
END;
$$^;

CREATE OR REPLACE TRIGGER trg_insert_metadata_vng
    AFTER INSERT ON versioned_named_graph
    FOR EACH ROW
EXECUTE FUNCTION trg_fn_insert_metadata_vng()^;
