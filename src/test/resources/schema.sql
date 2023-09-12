DROP TABLE IF EXISTS versioned_quad;
DROP TABLE IF EXISTS versioned_named_graph;
DROP INDEX IF EXISTS resource_or_literal_idx;
DROP TABLE IF EXISTS resource_or_literal;
DROP TABLE IF EXISTS version;

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

CREATE TABLE IF NOT EXISTS version
(
    index_version integer GENERATED ALWAYS AS IDENTITY,
    message   varchar(255),
    begin_version_date timestamptz default current_timestamp,
    end_version_date timestamptz default NULL,
    PRIMARY KEY (index_version)
);

-- TODO : Think about the alternative city version graph model
-- CREATE TABLE IF NOT EXISTS version_links
-- (
--     index_version integer,
--     index_version_parent integer,
--     PRIMARY KEY (index_version),
--     CONSTRAINT fk_version_parent
--         FOREIGN KEY (index_version_parent)
--             REFERENCES version_links (index_version),
--     CONSTRAINT fk_version
--         FOREIGN KEY (index_version)
--             REFERENCES version (index_version)
-- );
