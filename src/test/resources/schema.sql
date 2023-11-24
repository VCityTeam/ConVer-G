DROP INDEX IF EXISTS versioned_quad_ng_s_p_o;
DROP INDEX IF EXISTS versioned_quad_ng_s_o_p;
DROP INDEX IF EXISTS versioned_quad_ng_p_o_s;
DROP INDEX IF EXISTS versioned_quad_ng_p_s_o;
DROP INDEX IF EXISTS versioned_quad_ng_o_p_s;
DROP INDEX IF EXISTS versioned_quad_ng_o_s_p;
DROP TABLE IF EXISTS versioned_quad;
DROP TABLE IF EXISTS versioned_named_graph;
DROP INDEX IF EXISTS resource_or_literal_idx;
DROP TABLE IF EXISTS resource_or_literal;
DROP TABLE IF EXISTS version;
DROP TABLE IF EXISTS versioned_workspace;
DROP TABLE IF EXISTS workspace_version;

CREATE TABLE IF NOT EXISTS versioned_named_graph
(
    id_named_graph integer PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    name           varchar(255) UNIQUE,
    validity       bit varying
);

CREATE TABLE IF NOT EXISTS resource_or_literal
(
    id_resource_or_literal integer PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    name                   text,
    type                   varchar(255)
);

CREATE UNIQUE INDEX IF NOT EXISTS resource_or_literal_idx ON resource_or_literal (sha512(name::bytea), type) NULLS NOT DISTINCT;

CREATE TABLE IF NOT EXISTS versioned_quad
(
    id_object      integer,
    id_property    integer,
    id_subject     integer,
    id_named_graph integer,
    validity       bit varying,
    PRIMARY KEY (id_object, id_property, id_subject, id_named_graph),
    CONSTRAINT fk_named_graph
        FOREIGN KEY (id_named_graph)
            REFERENCES versioned_named_graph (id_named_graph)
);

CREATE INDEX IF NOT EXISTS versioned_quad_ng_s_p_o ON versioned_quad (id_named_graph, id_subject, id_property, id_object);
CREATE INDEX IF NOT EXISTS versioned_quad_ng_s_o_p ON versioned_quad (id_named_graph, id_subject, id_object, id_property);
CREATE INDEX IF NOT EXISTS versioned_quad_ng_p_o_s ON versioned_quad (id_named_graph, id_property, id_object, id_subject);
CREATE INDEX IF NOT EXISTS versioned_quad_ng_p_s_o ON versioned_quad (id_named_graph, id_property, id_subject, id_object);
CREATE INDEX IF NOT EXISTS versioned_quad_ng_o_p_s ON versioned_quad (id_named_graph, id_object, id_property, id_subject);
CREATE INDEX IF NOT EXISTS versioned_quad_ng_o_s_p ON versioned_quad (id_named_graph, id_object, id_subject, id_property);

CREATE TABLE IF NOT EXISTS version
(
    index_version integer GENERATED ALWAYS AS IDENTITY,
    message   varchar(255),
    begin_version_date timestamptz default current_timestamp,
    end_version_date timestamptz default NULL,
    PRIMARY KEY (index_version)
);

CREATE TABLE IF NOT EXISTS versioned_workspace
(
    id_object      integer,
    id_property    integer,
    id_subject     integer,
    validity       bit varying,
    PRIMARY KEY (id_object, id_property, id_subject)
);

CREATE TABLE IF NOT EXISTS workspace_version
(
    index_workspace_version integer GENERATED ALWAYS AS IDENTITY,
    message   varchar(255),
    begin_workspace_version_date timestamptz default current_timestamp,
    end_workspace_version_date timestamptz default NULL,
    PRIMARY KEY (index_workspace_version)
);
