package fr.vcity.sparqltosql.repository;

import fr.vcity.sparqltosql.dao.RDFCommit;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IRDFCommitRepository extends CrudRepository<RDFCommit, Integer> {

    @Query("""
    INSERT INTO commit VALUES (DEFAULT, :message, DEFAULT)
    RETURNING *
    """)
    RDFCommit save(String message);
}
