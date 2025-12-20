package fr.vcity.converg.repository;

import fr.vcity.converg.dao.VersionedQuad;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface IVersionedQuadRepository extends CrudRepository<VersionedQuad, Integer> {

    @Query(value = """
            UPDATE versioned_quad v
            SET validity = v.validity || B'0'
            WHERE bit_length(v.validity) = :version
            """)
    @Modifying
    void updateValidityVersionedQuad(@Param("version") Integer version);

    @Query(value = """
            INSERT INTO versioned_quad (id_subject, id_predicate, id_object, id_named_graph, validity)
                SELECT rls.id_resource_or_literal as id_subject,
                       rlp.id_resource_or_literal as id_predicate,
                       rlo.id_resource_or_literal as id_object,
                       rln.id_resource_or_literal as id_named_graph,
                       CAST(:newBitMask as bit varying) as val
                FROM flat_model_quad fm1
                         JOIN resource_or_literal rls ON sha512(rls.name::bytea) = sha512(fm1.subject::bytea) AND rls.name = fm1.subject AND (
                    fm1.subject_type IS NULL OR rls.type = fm1.subject_type
                    )
                         JOIN resource_or_literal rlp ON sha512(rlp.name::bytea) = sha512(fm1.predicate::bytea) AND rlp.name = fm1.predicate AND (
                    fm1.predicate_type IS NULL OR rlp.type = fm1.predicate_type
                    )
                         JOIN resource_or_literal rlo ON sha512(rlo.name::bytea) = sha512(fm1.object::bytea) AND rlo.name = fm1.object AND (
                    fm1.object_type IS NULL OR rlo.type = fm1.object_type
                    )
                         JOIN resource_or_literal rln ON sha512(rln.name::bytea) = sha512(fm1.named_graph::bytea) AND rln.name = fm1.named_graph AND rln.type IS NULL
            ON CONFLICT (id_subject, id_predicate, id_object, id_named_graph)
                DO UPDATE SET validity = overlay(EXCLUDED.validity placing versioned_quad.validity from 1)
            """)
    @Modifying
    void condenseModel(@Param("newBitMask") String newBitMask);
}
