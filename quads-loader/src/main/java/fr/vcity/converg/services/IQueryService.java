package fr.vcity.converg.services;

import fr.vcity.converg.dto.CompleteVersionedQuad;
import fr.vcity.converg.dto.Workspace;

import java.util.List;

public interface IQueryService {
    List<CompleteVersionedQuad> queryRequestedValidity(String requestedVersions);

    List<CompleteVersionedQuad> queryRequestedVersion(Integer requestedVersion);

    Workspace getGraphVersion();
}
