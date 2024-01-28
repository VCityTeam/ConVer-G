-- # Query the graph store for all quads
SELECT rl1.name as subject, rl2.name as predicate, rl3.name as object, rl4.name as graph -- récupération des noms des ressources (URI ou litéral)
FROM (
     SELECT t1.id_subject as v$subject,
            t1.id_property as v$predicate,
            t1.id_object as v$object,
            t1.id_named_graph as gn$graph,
            t1.validity as bs$graph
     FROM versioned_quad t1 -- contexte de quad (OpGraph)
) sq1
JOIN resource_or_literal rl1 ON rl1.id_resource_or_literal = sq1.v$subject AND rl1.name = 'https://dataset-dl.liris.cnrs.fr/rdf-owl-urban-data-ontologies/Datasets/GratteCiel_Workspace_2009_2018/3.0/GratteCiel#VILLEURBANNE_00067_5'
JOIN resource_or_literal rl2 ON rl2.id_resource_or_literal = sq1.v$predicate
JOIN resource_or_literal rl3 ON rl3.id_resource_or_literal = sq1.v$object
JOIN versioned_named_graph vng1 ON sq1.gn$graph = vng1.id_named_graph AND get_bit(sq1.bs$graph, vng1.index_version) = 1
JOIN resource_or_literal rl4 ON rl4.id_resource_or_literal = vng1.id_versioned_named_graph;