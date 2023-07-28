package fr.vcity.sparqltosql.services;

import fr.vcity.sparqltosql.dto.RDFCompleteVersionedQuad;
import fr.vcity.sparqltosql.dto.VersionAncestry;

import java.util.List;

public interface IQuadQueryService {
    List<RDFCompleteVersionedQuad> queryRequestedValidity(String requestedVersions);

    List<RDFCompleteVersionedQuad> queryRequestedVersion(Integer requestedVersion);

    List<RDFCompleteVersionedQuad> querySPARQL(String queryString);

    List<VersionAncestry> getGraphVersion();

    String getHashOfVersion(Integer indexVersion);
}
