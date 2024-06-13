DROP INDEX IF EXISTS versioned_quad_ng_s_p_o;
DROP INDEX IF EXISTS versioned_quad_ng_s_o_p;
DROP INDEX IF EXISTS versioned_quad_ng_p_o_s;
DROP INDEX IF EXISTS versioned_quad_ng_p_s_o;
DROP INDEX IF EXISTS versioned_quad_ng_o_p_s;
DROP INDEX IF EXISTS versioned_quad_ng_o_s_p;
DROP TABLE IF EXISTS versioned_quad CASCADE;
DROP TABLE IF EXISTS versioned_named_graph CASCADE;
DROP INDEX IF EXISTS resource_or_literal_idx;
DROP TABLE IF EXISTS resource_or_literal CASCADE;
DROP TABLE IF EXISTS version CASCADE;
DROP TABLE IF EXISTS metadata CASCADE;
DROP FUNCTION IF EXISTS version_named_graph;
DROP FUNCTION IF EXISTS add_quad;
DROP FUNCTION IF EXISTS add_triple;

CREATE TABLE IF NOT EXISTS resource_or_literal
(
    id_resource_or_literal integer PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    name                   text,
    type                   varchar(255)
);

CREATE UNIQUE INDEX IF NOT EXISTS resource_or_literal_idx ON resource_or_literal (sha512(name::bytea), type) NULLS NOT DISTINCT;

CREATE TABLE IF NOT EXISTS versioned_named_graph
(
    id_versioned_named_graph integer PRIMARY KEY REFERENCES resource_or_literal (id_resource_or_literal),
    id_named_graph           integer REFERENCES resource_or_literal (id_resource_or_literal),
    index_version            integer
);

CREATE TABLE IF NOT EXISTS versioned_quad
(
    id_object      integer REFERENCES resource_or_literal (id_resource_or_literal),
    id_predicate    integer REFERENCES resource_or_literal (id_resource_or_literal),
    id_subject     integer REFERENCES resource_or_literal (id_resource_or_literal),
    id_named_graph integer REFERENCES resource_or_literal (id_resource_or_literal),
    validity       bit varying,
    PRIMARY KEY (id_object, id_predicate, id_subject, id_named_graph)
);

CREATE INDEX IF NOT EXISTS versioned_quad_ng_s_p_o ON versioned_quad (id_named_graph, id_subject, id_predicate, id_object);
CREATE INDEX IF NOT EXISTS versioned_quad_ng_s_o_p ON versioned_quad (id_named_graph, id_subject, id_object, id_predicate);
CREATE INDEX IF NOT EXISTS versioned_quad_ng_p_o_s ON versioned_quad (id_named_graph, id_predicate, id_object, id_subject);
CREATE INDEX IF NOT EXISTS versioned_quad_ng_p_s_o ON versioned_quad (id_named_graph, id_predicate, id_subject, id_object);
CREATE INDEX IF NOT EXISTS versioned_quad_ng_o_p_s ON versioned_quad (id_named_graph, id_object, id_predicate, id_subject);
CREATE INDEX IF NOT EXISTS versioned_quad_ng_o_s_p ON versioned_quad (id_named_graph, id_object, id_subject, id_predicate);

CREATE TABLE IF NOT EXISTS version
(
    index_version          integer PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    message                varchar(255),
    transaction_time_start timestamptz default current_timestamp,
    transaction_time_end   timestamptz default NULL
);

CREATE TABLE IF NOT EXISTS metadata
(
    id_object   integer REFERENCES resource_or_literal (id_resource_or_literal),
    id_predicate integer REFERENCES resource_or_literal (id_resource_or_literal),
    id_subject  integer REFERENCES resource_or_literal (id_resource_or_literal),
    PRIMARY KEY (id_object, id_predicate, id_subject)
);

CREATE OR REPLACE FUNCTION version_named_graph(
    named_graph varchar,
    filename varchar,
    version integer
)
    RETURNS TABLE
            (
                id_named_graph         integer,
                id_version             integer,
                id_version_named_graph integer
            )
    LANGUAGE plpgsql
AS
'
    DECLARE
    BEGIN
        RETURN QUERY
            WITH ng AS (INSERT INTO resource_or_literal
                VALUES (DEFAULT, named_graph, NULL)
                ON CONFLICT (sha512(name::bytea), type) DO UPDATE SET type = EXCLUDED.type
                RETURNING *),
                 v AS (INSERT INTO resource_or_literal
                     VALUES (DEFAULT, ''https://github.com/VCityTeam/ConVer-G/Version#'' || filename, NULL)
                     ON CONFLICT (sha512(name::bytea), type) DO UPDATE SET type = EXCLUDED.type
                     RETURNING *),
                 vng AS (INSERT INTO resource_or_literal
                     VALUES (DEFAULT, ''https://github.com/VCityTeam/ConVer-G/Versioned-Named-Graph#'' ||
                                      encode(sha512((named_graph || filename)::bytea), ''hex''), NULL)
                     ON CONFLICT (sha512(name::bytea), type) DO UPDATE SET type = EXCLUDED.type
                     RETURNING *),
                 versioned AS (INSERT INTO versioned_named_graph
                     VALUES ((SELECT id_resource_or_literal FROM vng), (SELECT id_resource_or_literal FROM ng), version)
                     ON CONFLICT (id_versioned_named_graph) DO UPDATE SET id_named_graph = EXCLUDED.id_named_graph
                     RETURNING *),
                 metadata AS (INSERT INTO metadata (id_subject, id_predicate, id_object)
                     VALUES ((SELECT id_resource_or_literal FROM vng), (SELECT id_resource_or_literal
                                                                        FROM resource_or_literal
                                                                        WHERE name = ''https://github.com/VCityTeam/ConVer-G/Version#is-version-of''),
                             (SELECT id_resource_or_literal FROM ng)),
                            ((SELECT id_resource_or_literal FROM vng), (SELECT id_resource_or_literal
                                                                        FROM resource_or_literal
                                                                        WHERE name = ''https://github.com/VCityTeam/ConVer-G/Version#is-in-version''),
                             (SELECT v.id_resource_or_literal FROM v))
                     ON CONFLICT ON CONSTRAINT metadata_pkey
                         DO UPDATE SET id_subject = EXCLUDED.id_subject
                     RETURNING *
                 ),
                 result AS (
                     SELECT ng.id_resource_or_literal as id_named_graph, v.id_resource_or_literal as id_version, vng.id_resource_or_literal as id_versioned_named_graph
                     FROM ng, v, vng
                 )
                TABLE result;
    END;
';

CREATE OR REPLACE FUNCTION add_quad(
    subject varchar, subject_type varchar,
    predicate varchar, predicate_type varchar,
    object varchar, object_type varchar,
    named_graph varchar,
    version integer
)
    RETURNS setof versioned_quad
    LANGUAGE plpgsql
AS
'
    DECLARE
    BEGIN
        RETURN QUERY
            WITH vng AS (SELECT id_resource_or_literal
                         FROM resource_or_literal
                         WHERE name = named_graph AND type IS NULL
            ),
                 s AS (
                     SELECT id_resource_or_literal
                     FROM resource_or_literal
                     WHERE name = subject AND (
                         subject_type IS NULL OR type = subject_type
                         )),
                 p AS (SELECT id_resource_or_literal
                       FROM resource_or_literal
                       WHERE name = predicate AND (
                           predicate_type IS NULL OR type = predicate_type
                           )),
                 o AS (SELECT id_resource_or_literal
                       FROM resource_or_literal
                       WHERE name = object AND (
                           object_type IS NULL OR type = object_type
                           )),
                 q AS (INSERT INTO versioned_quad (id_subject, id_predicate, id_object, id_named_graph, validity)
                     SELECT s.id_resource_or_literal, p.id_resource_or_literal, o.id_resource_or_literal, vng.id_resource_or_literal, LPAD('''', version, ''0'')::bit varying || B''1''
                     FROM s, p, o, vng
                     ON CONFLICT ON CONSTRAINT versioned_quad_pkey
                         DO UPDATE SET validity = versioned_quad.validity || B''1''
                     RETURNING *)
                TABLE q;
    END;
';

CREATE OR REPLACE FUNCTION add_triple(
    subject varchar, subject_type varchar,
    property varchar, predicate_type varchar,
    object varchar, object_type varchar
)
    RETURNS setof metadata
    LANGUAGE plpgsql
AS
'
    DECLARE
    BEGIN
        RETURN QUERY
            WITH s AS (
                SELECT id_resource_or_literal
                FROM resource_or_literal
                WHERE name = subject AND (
                    subject_type IS NULL OR type = subject_type
                    )),
                 p AS (SELECT id_resource_or_literal
                       FROM resource_or_literal
                       WHERE name = property AND (
                           predicate_type IS NULL OR type = predicate_type
                           )),
                 o AS (SELECT id_resource_or_literal
                       FROM resource_or_literal
                       WHERE name = object AND (
                           object_type IS NULL OR type = object_type
                           )),
                 q AS (INSERT INTO metadata (id_subject, id_predicate, id_object)
                     SELECT s.id_resource_or_literal, p.id_resource_or_literal, o.id_resource_or_literal
                     FROM s, p, o
                     ON CONFLICT ON CONSTRAINT metadata_pkey
                         DO UPDATE SET id_subject = EXCLUDED.id_subject
                     RETURNING *)
                TABLE q;
    END;
';