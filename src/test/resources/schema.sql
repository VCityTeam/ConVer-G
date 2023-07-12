DROP TABLE IF EXISTS versioned_quad;
DROP TABLE IF EXISTS named_graph;
DROP TABLE IF EXISTS resource_or_literal;
DROP INDEX IF EXISTS resource_or_literal_idx;
DROP TABLE IF EXISTS commit;

CREATE TABLE IF NOT EXISTS named_graph
(
    id_named_graph integer PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    name           varchar(255) UNIQUE
);

CREATE TABLE IF NOT EXISTS resource_or_literal
(
    id_resource_or_literal integer PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    name                   text,
    type                   varchar(255)
);

CREATE UNIQUE INDEX IF NOT EXISTS resource_or_literal_idx ON resource_or_literal (name, type);

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
            REFERENCES named_graph (id_named_graph)
);

CREATE TABLE IF NOT EXISTS commit
(
    id_commit integer PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    message   varchar(255),
    date_commit timestamptz default current_timestamp
)