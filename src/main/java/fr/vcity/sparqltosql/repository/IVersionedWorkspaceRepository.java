package fr.vcity.sparqltosql.repository;

import fr.vcity.sparqltosql.dao.ScenarioVersion;
import fr.vcity.sparqltosql.dao.VersionedWorkspace;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IVersionedWorkspaceRepository extends CrudRepository<VersionedWorkspace, Integer> {

    @Query(value = """
            UPDATE versioned_workspace v
            SET validity = CASE
                WHEN bit_length(v.validity) = (
                    SELECT MAX(bit_length(v1.validity)) FROM versioned_workspace v1
                ) THEN v.validity
                ELSE v.validity || B'0'
            END
            WHERE bit_length(v.validity) = (
                SELECT MIN(bit_length(v1.validity)) FROM versioned_workspace v1
            )
            """)
    @Modifying
    void updateValidityVersionedWorkspace();

    @Query(value = """
        WITH version_transitions AS (
        SELECT rl18.name as space_type, rl4.name as scenario, rl6.name as version_transition, rl9.name as version_from, rl12.name as version_to
        FROM versioned_workspace vw
                 JOIN resource_or_literal rl1 ON vw.id_subject = rl1.id_resource_or_literal
                 JOIN resource_or_literal rl2 ON vw.id_property = rl2.id_resource_or_literal
                 JOIN resource_or_literal rl3 ON vw.id_object = rl3.id_resource_or_literal
                 JOIN versioned_workspace vw2 on vw.id_subject = vw2.id_subject
                 JOIN resource_or_literal rl4 ON vw2.id_subject = rl4.id_resource_or_literal
                 JOIN resource_or_literal rl5 ON vw2.id_property = rl5.id_resource_or_literal
                 JOIN resource_or_literal rl6 ON vw2.id_object = rl6.id_resource_or_literal
                 JOIN versioned_workspace vw3 on vw2.id_object = vw3.id_subject
                 JOIN resource_or_literal rl7 ON vw3.id_subject = rl7.id_resource_or_literal
                 JOIN resource_or_literal rl8 ON vw3.id_property = rl8.id_resource_or_literal
                 JOIN resource_or_literal rl9 ON vw3.id_object = rl9.id_resource_or_literal
                 JOIN versioned_workspace vw4 on vw2.id_object = vw4.id_subject
                 JOIN resource_or_literal rl10 ON vw4.id_subject = rl10.id_resource_or_literal
                 JOIN resource_or_literal rl11 ON vw4.id_property = rl11.id_resource_or_literal
                 JOIN resource_or_literal rl12 ON vw4.id_object = rl12.id_resource_or_literal
                 JOIN versioned_workspace vw5 ON vw2.id_subject = vw5.id_object
                 JOIN resource_or_literal rl13 ON vw5.id_subject = rl13.id_resource_or_literal
                 JOIN resource_or_literal rl14 ON vw5.id_property = rl14.id_resource_or_literal
                 JOIN resource_or_literal rl15 ON vw5.id_object = rl15.id_resource_or_literal
                 JOIN versioned_workspace vw6 ON vw5.id_subject = vw6.id_subject
                 JOIN resource_or_literal rl16 ON vw6.id_subject = rl16.id_resource_or_literal
                 JOIN resource_or_literal rl17 ON vw6.id_property = rl17.id_resource_or_literal
                 JOIN resource_or_literal rl18 ON vw6.id_object = rl18.id_resource_or_literal
        WHERE rl3.name = 'https://dataset-dl.liris.cnrs.fr/rdf-owl-urban-data-ontologies/Ontologies/Workspace/3.0/workspace#Scenario'
          AND rl5.name = 'https://dataset-dl.liris.cnrs.fr/rdf-owl-urban-data-ontologies/Ontologies/Workspace/3.0/workspace#Scenario.versionTransitionMember'
          AND rl8.name = 'https://dataset-dl.liris.cnrs.fr/rdf-owl-urban-data-ontologies/Ontologies/CityGML/3.0/versioning#VersionTransition.from'
          AND rl11.name = 'https://dataset-dl.liris.cnrs.fr/rdf-owl-urban-data-ontologies/Ontologies/CityGML/3.0/versioning#VersionTransition.to'
          AND rl17.name = 'http://www.w3.org/1999/02/22-rdf-syntax-ns#type'
          AND rl18.name LIKE 'https://dataset-dl.liris.cnrs.fr/rdf-owl-urban-data-ontologies/Ontologies/Workspace/3.0/workspace#%'
        ORDER BY space_type, scenario, version_transition
        ) SELECT vt2.space_type, vt2.scenario, vt1.version_from as previous_version, vt2.version_from as version, vt2.version_to as next_version FROM version_transitions vt1
            RIGHT JOIN version_transitions vt2 ON vt1.space_type = vt2.space_type AND vt1.scenario = vt2.scenario AND vt1.version_to = vt2.version_from;
    """)
    List<ScenarioVersion> getAllScenarios();
}
