package fr.vcity.sparqltosql.repository;

import fr.vcity.sparqltosql.dao.WorkspaceVersion;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IWorkspaceVersionRepository extends CrudRepository<WorkspaceVersion, Integer> {

    @Query("""
    INSERT INTO workspace_version VALUES (DEFAULT, :message, DEFAULT, DEFAULT)
    RETURNING *
    """)
    WorkspaceVersion save(String message);
}
