package fr.vcity.converg.repository;

import fr.vcity.converg.dao.Metadata;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IMetadataRepository extends CrudRepository<Metadata, Integer> {

    @Query(value = """
        INSERT INTO metadata (id_subject, id_predicate, id_object)
                SELECT rls.id_resource_or_literal as id_subject,
                       rlp.id_resource_or_literal as id_predicate,
                       rlo.id_resource_or_literal as id_object
                FROM flat_model_triple fm1
                        JOIN resource_or_literal rls ON rls.name = fm1.subject AND (
                    fm1.subject_type IS NULL OR rls.type = fm1.subject_type)
                        JOIN resource_or_literal rlp ON rlp.name = fm1.predicate AND (
                    fm1.predicate_type IS NULL OR rlp.type = fm1.predicate_type)
                        JOIN resource_or_literal rlo ON rlo.name = fm1.object AND (
                    fm1.object_type IS NULL OR rlo.type = fm1.object_type)
        ON CONFLICT (id_subject, id_predicate, id_object) DO NOTHING
        """)
    @Modifying
    void insertMetadataTriples();

}
