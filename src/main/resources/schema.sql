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
    index_version integer PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    message   varchar(255),
    begin_version_date timestamptz default current_timestamp,
    end_version_date timestamptz default NULL
)