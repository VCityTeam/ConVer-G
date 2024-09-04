package fr.vcity.converg.repository;

import fr.vcity.converg.dao.VersionedQuad;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IVersionedQuadRepository extends CrudRepository<VersionedQuad, Integer> {

    @Query(value = """
            UPDATE versioned_quad v
            SET validity = CASE
                WHEN bit_length(v.validity) = (
                    SELECT MAX(bit_length(v1.validity)) FROM versioned_quad v1
                ) THEN v.validity
                ELSE v.validity || B'0'
            END
            WHERE bit_length(v.validity) = (
                SELECT MIN(bit_length(v1.validity)) FROM versioned_quad v1
            )
            """)
    @Modifying
    void updateValidityVersionedQuad();

    @Query(value = """
            INSERT INTO versioned_quad (id_subject, id_predicate, id_object, id_named_graph, validity)
            SELECT fm.id_subject,
                   fm.id_predicate,
                   fm.id_object,
                   fm.id_named_graph,
                   bit_or(bs)::bit varying as val
            FROM (SELECT
                     rls.id_resource_or_literal as id_subject,
                     rlp.id_resource_or_literal as id_predicate,
                     rlo.id_resource_or_literal as id_object,
                     rln.id_resource_or_literal as id_named_graph,
                     set_bit(
                             repeat('0', (SELECT MAX(fm2.version)
                                          FROM flat_model_quad fm2) + 1
                             )::bit varying, fm1.version, 1
                     ) as bs
                  FROM flat_model_quad fm1
                           JOIN resource_or_literal rls ON fm1.subject = rls.name AND (
                      fm1.subject_type IS NULL OR rls.type = fm1.subject_type
                      )
                           JOIN resource_or_literal rlp ON fm1.predicate = rlp.name AND (
                      fm1.predicate_type IS NULL OR rlp.type = fm1.predicate_type
                      )
                           JOIN resource_or_literal rlo ON fm1.object = rlo.name AND (
                      fm1.object_type IS NULL OR rlo.type = fm1.object_type
                      )
                           JOIN resource_or_literal rln ON fm1.named_graph = rln.name AND rln.type IS NULL
                  GROUP BY rls.id_resource_or_literal,
                           rlp.id_resource_or_literal,
                           rlo.id_resource_or_literal,
                           rln.id_resource_or_literal,
                           fm1.version) fm
            GROUP BY fm.id_subject,
                     fm.id_predicate,
                     fm.id_object,
                     fm.id_named_graph
            ON CONFLICT (id_subject, id_predicate, id_object, id_named_graph)
            DO UPDATE SET validity = overlay(EXCLUDED.validity placing versioned_quad.validity from 1)
            """)
    @Modifying
    void condenseModel();
    // TODO: Add the option to import another dataset after a condensation (ON CONFLICT)
}
