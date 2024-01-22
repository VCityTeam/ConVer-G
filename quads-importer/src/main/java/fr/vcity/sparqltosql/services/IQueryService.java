package fr.vcity.sparqltosql.services;

import fr.vcity.sparqltosql.dto.CompleteVersionedQuad;
import fr.vcity.sparqltosql.dto.Workspace;
import org.apache.jena.query.Query;

import java.util.List;

public interface IQueryService {
    List<CompleteVersionedQuad> queryRequestedValidity(String requestedVersions);

    List<CompleteVersionedQuad> queryRequestedVersion(Integer requestedVersion);

    List<CompleteVersionedQuad> querySPARQL(String queryString);

    List<CompleteVersionedQuad> getOperatorsFromQueryARQ(Query query);

    Workspace getGraphVersion();
}
