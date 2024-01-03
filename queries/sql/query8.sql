-- # Common prefixes
-- PREFIX owl:    <http://www.w3.org/2002/07/owl#>
--
-- # CityGML 2.0 prefixes
-- PREFIX core: <https://dataset-dl.liris.cnrs.fr/rdf-owl-urban-data-ontologies/Ontologies/CityGML/3.0/core#>
-- PREFIX bldg: <https://dataset-dl.liris.cnrs.fr/rdf-owl-urban-data-ontologies/Ontologies/CityGML/3.0/building#>
--
-- # Dataset prefixes
-- PREFIX vt:    <https://dataset-dl.liris.cnrs.fr/rdf-owl-urban-data-ontologies/Datasets/GratteCiel_Workspace_2009_2018/3.0/GratteCiel#>
--
-- # Return the number of features within a version
-- SELECT (COUNT(?object) AS ?number)
-- WHERE {
--     ?v <https://github.com/VCityTeam/SPARQL-to-SQL#isInVersion> vt:version_2012 .
--     GRAPH vt:version_2012 {
--     ?subject a core:CityModel ;
-- ?predicate ?object ;
-- a ?subjectType .
--       ?object a bldg:Building .
--       ?object a ?objectType .
--
--       FILTER(?subjectType != owl:NamedIndividual)
--       FILTER(?objectType != owl:NamedIndividual)
--     }
-- }


SELECT COUNT(rl1.name) as number
FROM versioned_quad vq1
         -- a bldg:Building
         JOIN versioned_quad vq2 ON vq1.id_subject = vq2.id_subject
         JOIN resource_or_literal rl1 ON vq1.id_subject = rl1.id_resource_or_literal
         JOIN resource_or_literal rl2 ON vq1.id_property = rl2.id_resource_or_literal
         JOIN resource_or_literal rl3 ON vq1.id_object = rl3.id_resource_or_literal
         --
WHERE rl2.name = 'http://www.w3.org/1999/02/22-rdf-syntax-ns#type'
  AND rl3.name = 'https://dataset-dl.liris.cnrs.fr/rdf-owl-urban-data-ontologies/Ontologies/CityGML/3.0/building#Building'