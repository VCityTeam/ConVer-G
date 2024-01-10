package fr.vcity.sparqltosql.services;

import fr.vcity.sparqltosql.dto.RDFCompleteVersionedQuad;
import fr.vcity.sparqltosql.dto.Workspace;
import org.apache.jena.query.Query;

import java.util.List;

public interface IQueryService {
    List<RDFCompleteVersionedQuad> queryRequestedValidity(String requestedVersions);

    List<RDFCompleteVersionedQuad> queryRequestedVersion(Integer requestedVersion);

    List<RDFCompleteVersionedQuad> querySPARQL(String queryString);

    List<RDFCompleteVersionedQuad> getOperatorsFromQueryARQ(Query query);

    Workspace getGraphVersion();
}
