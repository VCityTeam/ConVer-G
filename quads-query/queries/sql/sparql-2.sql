-- # Query the graph store for all quads with a given subject

SELECT rl2.name as predicate, rl3.name as object, rl4.name as graph -- récupération des noms des ressources (URI ou litéral)
FROM (
     SELECT sq1.v$s, sq1.v$p, sq1.v$o, sq1.v$vg -- réalisation des projections (OpProject)
     FROM (
              SELECT t1.id_subject as "v$s", t1.id_property as "v$p", t1.id_object as "v$o", vng1.id_versioned_named_graph as "v$vg"
              FROM versioned_quad t1 -- contexte de quad (OpGraph)
              JOIN versioned_named_graph vng1 ON vng1.id_named_graph = t1.id_named_graph
     ) sq1
 ) sq2
 JOIN resource_or_literal rl1 ON rl1.id_resource_or_literal = sq2.v$s AND rl1.name = 'https://dataset-dl.liris.cnrs.fr/rdf-owl-urban-data-ontologies/Datasets/GratteCiel_Workspace_2009_2018/3.0/GratteCiel#VILLEURBANNE_00067_5'
 JOIN resource_or_literal rl2 ON rl2.id_resource_or_literal = sq2.v$p
 JOIN resource_or_literal rl3 ON rl3.id_resource_or_literal = sq2.v$o
 JOIN resource_or_literal rl4 ON rl4.id_resource_or_literal = sq2.v$vg;

