package fr.vcity.sparqltosql.repository;

import fr.vcity.sparqltosql.dao.VersionedQuad;
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
}
