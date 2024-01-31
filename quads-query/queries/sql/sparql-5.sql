-- # Query the graph store for all quads with a given subject and a join on the subject (predicate might be equal to predicate2 and object might be equal to object2)

SELECT rl1.name as subject, rl2.name as predicate, rl3.name as object, rl5.name as object2, rl4.name as graph -- récupération des noms des ressources (URI ou litéral)
FROM (
     SELECT sq1.v$s1, sq1.v$s2, sq1.v$p1, sq1.v$p2, sq1.v$o1, sq1.v$o2, sq1.v$vg1 -- réalisation des projections (OpProject)
     FROM (
              SELECT t1.id_subject as v$s1, t2.id_subject as v$s2, t1.id_property as v$p1, t2.id_property as v$p2, t1.id_object as v$o1, t2.id_object as v$o2, vng1.id_versioned_named_graph as v$vg1
              FROM versioned_quad t1 -- contexte de quad (OpGraph)
              JOIN versioned_quad t2 ON t2.id_object = t1.id_object
              JOIN versioned_named_graph vng1 ON vng1.id_named_graph = t1.id_named_graph
          ) sq1
 ) sq2
JOIN resource_or_literal rl1 ON rl1.id_resource_or_literal = sq2.v$s1 AND rl1.name = 'https://dataset-dl.liris.cnrs.fr/rdf-owl-urban-data-ontologies/Datasets/GratteCiel_Workspace_2009_2018/3.0/GratteCiel#VILLEURBANNE_00067_5'
JOIN resource_or_literal rl2 ON rl2.id_resource_or_literal = sq2.v$p1
JOIN resource_or_literal rl3 ON rl3.id_resource_or_literal = sq2.v$o1
JOIN resource_or_literal rl4 ON rl4.id_resource_or_literal = sq2.v$vg1
JOIN resource_or_literal rl5 ON rl5.id_resource_or_literal = sq2.v$o2;
