package fr.vcity.sparqltosql.repository;

import fr.vcity.sparqltosql.dao.ResourceOrLiteral;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IResourceOrLiteralRepository extends CrudRepository<ResourceOrLiteral, Integer> {

    @Query("""
    INSERT INTO resource_or_literal VALUES (DEFAULT, :name, :type)
    ON CONFLICT (sha512(name::bytea), type) DO UPDATE SET type = EXCLUDED.type
    RETURNING *
    """)
    ResourceOrLiteral save(String name, String type);

    @Query("""
    INSERT INTO resource_or_literal (name, type)
    SELECT fmq.subject, fmq.subject_type
        FROM flat_model_quad fmq
    ON CONFLICT DO NOTHING
    """)
    @Modifying
    void flatModelSubjectToCatalog();

    @Query("""
    INSERT INTO resource_or_literal (name, type)
    SELECT fmq.predicate, fmq.predicate_type
        FROM flat_model_quad fmq
    ON CONFLICT DO NOTHING
    """)
    @Modifying
    void flatModelPredicateToCatalog();

    @Query("""
    INSERT INTO resource_or_literal (name, type)
    SELECT fmq.object, fmq.object_type
        FROM flat_model_quad fmq
    ON CONFLICT DO NOTHING
    """)
    @Modifying
    void flatModelObjectToCatalog();
}
