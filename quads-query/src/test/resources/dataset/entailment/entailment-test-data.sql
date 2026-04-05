INSERT INTO resource_or_literal (id_resource_or_literal, name, type) OVERRIDING SYSTEM VALUE VALUES
    ( 1, 'http://example.edu/university', NULL),
    ( 2, 'http://www.w3.org/1999/02/22-rdf-syntax-ns#type', NULL),
    ( 3, 'http://www.w3.org/2000/01/rdf-schema#subClassOf', NULL),
    ( 4, 'http://www.w3.org/2000/01/rdf-schema#subPropertyOf', NULL),
    ( 5, 'http://www.w3.org/2000/01/rdf-schema#domain', NULL),
    ( 6, 'http://www.w3.org/2000/01/rdf-schema#range', NULL),
    ( 7, 'http://example.edu/Person', NULL),
    ( 8, 'http://example.edu/Student', NULL),
    ( 9, 'http://example.edu/GradStudent', NULL),
    (10, 'http://example.edu/PhDStudent', NULL),
    (11, 'http://example.edu/Course', NULL),
    (12, 'http://example.edu/alice', NULL),
    (13, 'http://example.edu/bob', NULL),
    (14, 'http://example.edu/charlie', NULL),
    (15, 'http://example.edu/prof1', NULL),
    (16, 'http://example.edu/prof2', NULL),
    (17, 'http://example.edu/CS101', NULL),
    (18, 'http://example.edu/enrolledIn', NULL),
    (19, 'http://example.edu/teaches', NULL),
    (20, 'http://example.edu/advises', NULL),
    (21, 'http://example.edu/relatedTo', NULL),
    (22, 'https://github.com/VCityTeam/ConVer-G/Versioned-Named-Graph#university-v1', NULL),
    (23, 'https://github.com/VCityTeam/ConVer-G/Versioned-Named-Graph#university-v2', NULL);

INSERT INTO version (index_version, message) OVERRIDING SYSTEM VALUE VALUES
    (1, 'https://github.com/VCityTeam/ConVer-G/Versioned-Named-Graph#university-v1'),
    (2, 'https://github.com/VCityTeam/ConVer-G/Versioned-Named-Graph#university-v2');

-- ---- Versioned Named Graphs ----
--   VNG 22 -> named graph 1, version 1
--   VNG 23 -> named graph 1, version 2
INSERT INTO versioned_named_graph (id_versioned_named_graph, id_named_graph, index_version) VALUES
    (22, 1, 1),
    (23, 1, 2);

-- ---- Versioned Quads (condensed mode) ----
-- Format: (id_object, id_predicate, id_subject, id_named_graph, validity)

-- Schema: class hierarchy
--   Student subClassOf Person                      v1+v2
INSERT INTO versioned_quad VALUES ( 7,  3,  8, 1, B'11');
--   GradStudent subClassOf Student                 v1+v2
INSERT INTO versioned_quad VALUES ( 8,  3,  9, 1, B'11');
--   PhDStudent subClassOf GradStudent              v2 only
INSERT INTO versioned_quad VALUES ( 9,  3, 10, 1, B'01');

-- Schema: property declarations
--   enrolledIn rdfs:domain Student                 v1+v2
INSERT INTO versioned_quad VALUES ( 8,  5, 18, 1, B'11');
--   enrolledIn rdfs:range Course                   v1+v2
INSERT INTO versioned_quad VALUES (11,  6, 18, 1, B'11');
--   teaches rdfs:subPropertyOf relatedTo           v1+v2
INSERT INTO versioned_quad VALUES (21,  4, 19, 1, B'11');
--   advises rdfs:subPropertyOf teaches             v2 only
INSERT INTO versioned_quad VALUES (19,  4, 20, 1, B'01');

-- Data: type assertions
--   alice rdf:type GradStudent                     v1+v2
INSERT INTO versioned_quad VALUES ( 9,  2, 12, 1, B'11');
--   bob rdf:type Student                           v1 only
INSERT INTO versioned_quad VALUES ( 8,  2, 13, 1, B'10');
--   charlie rdf:type PhDStudent                    v2 only
INSERT INTO versioned_quad VALUES (10,  2, 14, 1, B'01');

-- Data: property assertions
--   alice enrolledIn CS101                         v1+v2
INSERT INTO versioned_quad VALUES (17, 18, 12, 1, B'11');
--   prof1 teaches CS101                            v1+v2
INSERT INTO versioned_quad VALUES (17, 19, 15, 1, B'11');
--   prof2 advises charlie                          v2 only
INSERT INTO versioned_quad VALUES (14, 20, 16, 1, B'01');

-- ---- Versioned Quads (flat mode) ----
-- Version 1 (VNG=22): all triples with bit 0 set in validity above
INSERT INTO versioned_quad_flat VALUES ( 7,  3,  8, 22);  -- Student subClassOf Person
INSERT INTO versioned_quad_flat VALUES ( 8,  3,  9, 22);  -- GradStudent subClassOf Student
INSERT INTO versioned_quad_flat VALUES ( 8,  5, 18, 22);  -- enrolledIn domain Student
INSERT INTO versioned_quad_flat VALUES (11,  6, 18, 22);  -- enrolledIn range Course
INSERT INTO versioned_quad_flat VALUES (21,  4, 19, 22);  -- teaches subPropertyOf relatedTo
INSERT INTO versioned_quad_flat VALUES ( 9,  2, 12, 22);  -- alice type GradStudent
INSERT INTO versioned_quad_flat VALUES ( 8,  2, 13, 22);  -- bob type Student
INSERT INTO versioned_quad_flat VALUES (17, 18, 12, 22);  -- alice enrolledIn CS101
INSERT INTO versioned_quad_flat VALUES (17, 19, 15, 22);  -- prof1 teaches CS101

-- Version 2 (VNG=23): all triples with bit 1 set in validity above
INSERT INTO versioned_quad_flat VALUES ( 7,  3,  8, 23);  -- Student subClassOf Person
INSERT INTO versioned_quad_flat VALUES ( 8,  3,  9, 23);  -- GradStudent subClassOf Student
INSERT INTO versioned_quad_flat VALUES ( 9,  3, 10, 23);  -- PhDStudent subClassOf GradStudent
INSERT INTO versioned_quad_flat VALUES ( 8,  5, 18, 23);  -- enrolledIn domain Student
INSERT INTO versioned_quad_flat VALUES (11,  6, 18, 23);  -- enrolledIn range Course
INSERT INTO versioned_quad_flat VALUES (21,  4, 19, 23);  -- teaches subPropertyOf relatedTo
INSERT INTO versioned_quad_flat VALUES (19,  4, 20, 23);  -- advises subPropertyOf teaches
INSERT INTO versioned_quad_flat VALUES ( 9,  2, 12, 23);  -- alice type GradStudent
INSERT INTO versioned_quad_flat VALUES (10,  2, 14, 23);  -- charlie type PhDStudent
INSERT INTO versioned_quad_flat VALUES (17, 18, 12, 23);  -- alice enrolledIn CS101
INSERT INTO versioned_quad_flat VALUES (17, 19, 15, 23);  -- prof1 teaches CS101
INSERT INTO versioned_quad_flat VALUES (14, 20, 16, 23);  -- prof2 advises charlie
