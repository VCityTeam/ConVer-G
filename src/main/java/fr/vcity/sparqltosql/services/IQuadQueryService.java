package fr.vcity.sparqltosql.services;

import fr.vcity.sparqltosql.dto.RDFCompleteVersionedQuad;

import java.util.List;

public interface IQuadQueryService {
    List<RDFCompleteVersionedQuad> queryRequestedValidity(String requestedVersions);

    List<RDFCompleteVersionedQuad> queryRequestedVersion(Integer requestedVersion);

    List<RDFCompleteVersionedQuad> querySPARQL(String queryString);
}
