package fr.vcity.sparqltosql.repository;

import fr.vcity.sparqltosql.dao.RDFNamedGraph;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IRDFNamedGraphRepository extends CrudRepository<RDFNamedGraph, Integer> {

    @Query("""
    INSERT INTO named_graph VALUES (DEFAULT, :name)
    ON CONFLICT (name) DO NOTHING
    RETURNING *
    """)
    RDFNamedGraph save(String name);

    Optional<RDFNamedGraph> findByName(
            String name
    );
}
