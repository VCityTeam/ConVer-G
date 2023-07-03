-- Across multiple concurrent city versions, what is the maximum known height of a particular building?
SELECT MAX(rl3.name) as maximum
FROM versioned_quad vq
         LEFT JOIN resource_or_literal rl1 ON vq.id_subject = rl1.id_resource_or_literal
         LEFT JOIN resource_or_literal rl2 ON vq.id_property = rl2.id_resource_or_literal
         LEFT JOIN resource_or_literal rl3 ON vq.id_object = rl3.id_resource_or_literal
WHERE rl1.name = 'https://github.com/VCityTeam/UD-Graph/LYON_1ER_BATI_2015-1_bldg#BU_69381AB243_1'
  and rl2.name = 'http://www.opengis.net/citygml/building/2.0/building#AbstractBuilding.measuredHeight';


-- TODO : V2 Across multiple concurrent city versions, what is the maximum known height of a particular building?

SELECT MAX(rl5.name) as maximum
FROM versioned_quad vq1
         LEFT JOIN versioned_quad vq2 ON vq1.id_subject = vq2.id_subject
         LEFT JOIN resource_or_literal rl1 ON vq1.id_subject = rl1.id_resource_or_literal
         LEFT JOIN resource_or_literal rl2 ON vq1.id_property = rl2.id_resource_or_literal
         LEFT JOIN resource_or_literal rl3 ON vq1.id_object = rl3.id_resource_or_literal
         LEFT JOIN resource_or_literal rl4 ON vq2.id_property = rl4.id_resource_or_literal
         LEFT JOIN resource_or_literal rl5 ON vq2.id_object = rl5.id_resource_or_literal
WHERE rl1.name = 'https://github.com/VCityTeam/UD-Graph/LYON_1ER_BATI_2015-1_bldg#BU_69381AB243_1'
  AND rl2.name = 'http://www.w3.org/1999/02/22-rdf-syntax-ns#type'
  AND rl3.name = 'http://www.opengis.net/ont/geosparql#Geometry'
  and rl4.name = 'http://www.opengis.net/citygml/building/2.0/building#AbstractBuilding.measuredHeight'

-- Across multiple concurrent versions, find all the quads that were not valid once (or more) before the '2023-07-03 13:18'
SELECT (c.id_commit - 1) as version, rls.name, rlp.name, rlo.name
FROM versioned_quad v
         LEFT JOIN resource_or_literal rls ON rls.id_resource_or_literal = v.id_subject
         LEFT JOIN resource_or_literal rlp ON rlp.id_resource_or_literal = v.id_property
         LEFT JOIN resource_or_literal rlo ON rlo.id_resource_or_literal = v.id_object
         LEFT JOIN commit c ON c.date_commit < '2023-07-03 13:18'
WHERE get_bit(v.validity, c.id_commit - 1) = 0