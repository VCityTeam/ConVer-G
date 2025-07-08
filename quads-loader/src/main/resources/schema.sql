CREATE TABLE IF NOT EXISTS resource_or_literal
(
    id_resource_or_literal integer PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    name                   text,
    type                   varchar(255)
);

CREATE TABLE IF NOT EXISTS versioned_named_graph
(
    id_versioned_named_graph integer PRIMARY KEY REFERENCES resource_or_literal (id_resource_or_literal),
    id_named_graph           integer REFERENCES resource_or_literal (id_resource_or_literal),
    index_version            integer
);


CREATE UNIQUE INDEX IF NOT EXISTS resource_or_literal_idx ON resource_or_literal (sha512(name::bytea), type) NULLS NOT DISTINCT;

CREATE TABLE IF NOT EXISTS versioned_quad
(
    id_object      integer REFERENCES resource_or_literal (id_resource_or_literal),
    id_predicate   integer REFERENCES resource_or_literal (id_resource_or_literal),
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
);

CREATE TABLE IF NOT EXISTS flat_model_triple
(
    id_record      integer PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    subject        text,
    subject_type   varchar,
    predicate      text,
    predicate_type varchar,
    object         text,
    object_type    varchar
);

CREATE TABLE IF NOT EXISTS versioned_quad_flat
(
    id_object      integer REFERENCES resource_or_literal (id_resource_or_literal),
    id_predicate   integer REFERENCES resource_or_literal (id_resource_or_literal),
    id_subject     integer REFERENCES resource_or_literal (id_resource_or_literal),
    id_versioned_named_graph integer REFERENCES resource_or_literal (id_resource_or_literal),
    PRIMARY KEY (id_object, id_predicate, id_subject, id_versioned_named_graph)
);

CREATE INDEX IF NOT EXISTS versioned_quad_flat_vng_s_p_o ON versioned_quad_flat (id_versioned_named_graph, id_subject, id_predicate, id_object);
CREATE INDEX IF NOT EXISTS versioned_quad_flat_vng_s_o_p ON versioned_quad_flat (id_versioned_named_graph, id_subject, id_object, id_predicate);
CREATE INDEX IF NOT EXISTS versioned_quad_flat_vng_p_o_s ON versioned_quad_flat (id_versioned_named_graph, id_predicate, id_object, id_subject);
CREATE INDEX IF NOT EXISTS versioned_quad_flat_vng_p_s_o ON versioned_quad_flat (id_versioned_named_graph, id_predicate, id_subject, id_object);
CREATE INDEX IF NOT EXISTS versioned_quad_flat_vng_o_p_s ON versioned_quad_flat (id_versioned_named_graph, id_object, id_predicate, id_subject);
CREATE INDEX IF NOT EXISTS versioned_quad_flat_vng_o_s_p ON versioned_quad_flat (id_versioned_named_graph, id_object, id_subject, id_predicate);

CREATE TABLE IF NOT EXISTS version
(
    index_version          integer PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    message                varchar(255),
    transaction_time_start timestamptz default current_timestamp,
    transaction_time_end   timestamptz default NULL
);

CREATE TABLE IF NOT EXISTS metadata
(
    id_object    integer REFERENCES resource_or_literal (id_resource_or_literal),
    id_predicate integer REFERENCES resource_or_literal (id_resource_or_literal),
    id_subject   integer REFERENCES resource_or_literal (id_resource_or_literal),
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
