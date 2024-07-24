package fr.vcity.sparqltosql.services;

import fr.vcity.sparqltosql.dto.CompleteVersionedQuad;
import fr.vcity.sparqltosql.dto.Workspace;

import java.util.List;

public interface IQueryService {
    List<CompleteVersionedQuad> queryRequestedValidity(String requestedVersions);

    List<CompleteVersionedQuad> queryRequestedVersion(Integer requestedVersion);

    Workspace getGraphVersion();
}
