package fr.vcity.sparqltosql.repository;

import fr.vcity.sparqltosql.dao.Version;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IVersionRepository extends CrudRepository<Version, Integer> {

    @Query("""
    INSERT INTO version VALUES (DEFAULT, :message, DEFAULT, DEFAULT)
    RETURNING *
    """)
    Version save(String message);
}
