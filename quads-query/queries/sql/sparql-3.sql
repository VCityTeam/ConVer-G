-- # Query the graph store for all quads with a given subject and a join on the subject (predicate might be equal to predicate2 and object might be equal to object2)

SELECT rl2.name as predicate, rl3.name as object, rl4.name as graph -- récupération des noms des ressources (URI ou litéral)
FROM (
         SELECT t1.id_property as v$p, t1.id_object as v$o, t1.id_named_graph as gn$g, t1.validity & t2.validity as bs$g, t2.id_property as v$p2, t2.id_object as v$o2
         FROM versioned_quad t1, versioned_quad t2
         WHERE t1.id_subject = 1231796 AND t2.id_subject = 1231796
         AND (t1.validity & t2.validity) <> B'0'
     ) sq1
 JOIN resource_or_literal rl2 ON rl2.id_resource_or_literal = sq1.v$p
 JOIN resource_or_literal rl3 ON rl3.id_resource_or_literal = sq1.v$o
 JOIN versioned_named_graph vng1 ON sq1.gn$g = vng1.id_named_graph AND get_bit(sq1.bs$g, vng1.index_version) = 1
 JOIN resource_or_literal rl4 ON rl4.id_resource_or_literal = vng1.id_versioned_named_graph;

SELECT (t0.validity & t1.validity) as bs$g, t0.id_named_graph as ng$g,
       t0.id_subject as v$s1,
       t1.id_object as v$o2,
       t0.id_property as v$p1,
       t1.id_property as v$p2
FROM versioned_quad t0, versioned_quad t1
WHERE t0.id_named_graph = t1.id_named_graph AND bit_count(t0.validity & t1.validity) <> 0 AND t0.id_object = 102