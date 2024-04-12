-- # Query the graph store for all quads
SELECT rl1.name as subject, rl2.name as predicate, rl3.name as object, rl4.name as graph -- récupération des noms des ressources (URI ou litéral)
FROM (
     SELECT t1.id_subject as v$s, t1.id_property as v$p, t1.id_object as v$o, t1.id_named_graph as ng$g, t1.validity as bs$g
     FROM versioned_quad t1 -- contexte de quad (OpGraph)
) sq1
JOIN resource_or_literal rl1 ON rl1.id_resource_or_literal = sq1.v$s
JOIN resource_or_literal rl2 ON rl2.id_resource_or_literal = sq1.v$p
JOIN resource_or_literal rl3 ON rl3.id_resource_or_literal = sq1.v$o
JOIN versioned_named_graph vng1 ON sq1.ng$g = vng1.id_named_graph AND get_bit(sq1.bs$g, vng1.index_version) = 1
JOIN resource_or_literal rl4 ON rl4.id_resource_or_literal = vng1.id_versioned_named_graph;