-- # Query the workspace (default graph) for all triples
SELECT rl1.name as "subject", rl2.name as "predicate", rl3.name as "object" -- récupération des noms des ressources (URI ou litéral)
FROM (
     SELECT sq1."v$s", sq1."v$p", sq1."v$o" -- réalisation des projections (OpProject)
     FROM (
              SELECT t1.id_subject as "v$s", t1.id_property as "v$p", t1.id_object as "v$o"
              FROM workspace t1 -- contexte de triple (métadonnées)
          ) sq1
 ) sq2
 JOIN resource_or_literal rl1 ON rl1.id_resource_or_literal = sq2.v$s
 JOIN resource_or_literal rl2 ON rl2.id_resource_or_literal = sq2.v$p
 JOIN resource_or_literal rl3 ON rl3.id_resource_or_literal = sq2.v$o