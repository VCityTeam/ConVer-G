package fr.vcity.sparqltosql.repository;

import fr.vcity.sparqltosql.dao.RDFResourceOrLiteral;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IRDFResourceOrLiteralRepository extends CrudRepository<RDFResourceOrLiteral, Integer> {

    @Query("""
    INSERT INTO resource_or_literal VALUES (DEFAULT, :name, :type)
    ON CONFLICT (name, type) DO NOTHING
    RETURNING *
    """)
    RDFResourceOrLiteral save(String name, String type);

    Optional<RDFResourceOrLiteral> findByNameAndType(String name, String type);
}
