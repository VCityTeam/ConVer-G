-- # Query the workspace (default graph) for all triples
SELECT rl1.name as "s", rl2.name as "p", rl3.name as "o" -- récupération des noms des ressources (URI ou litéral)
FROM (
     SELECT t1.id_subject as w$s, t1.id_property as w$p, t1.id_object as w$o
     FROM workspace t1 -- contexte de triple (métadonnées)
 ) sq1
 JOIN resource_or_literal rl1 ON rl1.id_resource_or_literal = sq1.w$s
 JOIN resource_or_literal rl2 ON rl2.id_resource_or_literal = sq1.w$p
 JOIN resource_or_literal rl3 ON rl3.id_resource_or_literal = sq1.w$o