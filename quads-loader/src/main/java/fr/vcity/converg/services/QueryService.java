package fr.vcity.converg.services;

import fr.vcity.converg.dao.ScenarioVersion;
import fr.vcity.converg.dto.CompleteVersionedQuad;
import fr.vcity.converg.dto.Scenario;
import fr.vcity.converg.dto.Space;
import fr.vcity.converg.dto.Workspace;
import fr.vcity.converg.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class QueryService implements IQueryService {

    IResourceOrLiteralRepository rdfResourceRepository;
    IVersionedQuadRepository rdfVersionedQuadRepository;
    IVersionedNamedGraphRepository rdfNamedGraphRepository;
    IMetadataRepository metadataRepository;
    IVersionRepository versionRepository;
    VersionedQuadComponent versionedQuadComponent;

    public QueryService(
            IResourceOrLiteralRepository rdfResourceRepository,
            IVersionedQuadRepository rdfVersionedQuadRepository,
            IVersionedNamedGraphRepository rdfNamedGraphRepository,
            IVersionRepository versionRepository,
            VersionedQuadComponent versionedQuadComponent,
            IMetadataRepository metadataRepository
    ) {
        this.rdfResourceRepository = rdfResourceRepository;
        this.rdfVersionedQuadRepository = rdfVersionedQuadRepository;
        this.rdfNamedGraphRepository = rdfNamedGraphRepository;
        this.versionRepository = versionRepository;
        this.versionedQuadComponent = versionedQuadComponent;
        this.metadataRepository = metadataRepository;
    }

    /**
     * @param requestedValidity the request validity
     * @return the quads list filtered by requestedValidity
     */
    @Override
    public List<CompleteVersionedQuad> queryRequestedValidity(String requestedValidity) {
        log.debug("Requested: {}", requestedValidity);

        if (requestedValidity.equals("*")) {
            return versionedQuadComponent.findAll();
        }

        return versionedQuadComponent
                .findAllByValidity(requestedValidity);
    }

    /**
     * @param requestedVersion the request version number
     * @return the quads list filtered by requestedVersion where the fact has to be truthy
     */
    @Override
    public List<CompleteVersionedQuad> queryRequestedVersion(Integer requestedVersion) {
        log.debug("Requested version: {}", requestedVersion);

        return versionedQuadComponent.findAllByVersion(requestedVersion);
    }

    /**
     * Returns the whole graph version of the database
     */
    @Override
    public Workspace getGraphVersion() {
        List<ScenarioVersion> scenarios = metadataRepository.getAllScenarios();

        Workspace workspace = new Workspace();

        Map<String, List<ScenarioVersion>> spaceMap = scenarios
                .stream()
                .collect(Collectors.groupingBy(ScenarioVersion::getSpaceType));

        for (Map.Entry<String, List<ScenarioVersion>> entry : spaceMap.entrySet()) {
            String s = entry.getKey();
            List<ScenarioVersion> scenarioVersions = entry.getValue();
            Map<String, List<ScenarioVersion>> scenariosMap = scenarioVersions
                    .stream()
                    .filter(scenarioVersion -> scenarioVersion.getSpaceType().equals(s))
                    .collect(Collectors.groupingBy(ScenarioVersion::getScenario));

            List<Scenario> computedScenarioList = new ArrayList<>();

            scenariosMap.forEach((sc, scVersions) -> {
                ScenarioVersion currentSV = scenariosMap.get(sc).getFirst();

                List<String> versions = new ArrayList<>();
                while (currentSV != null) {
                    if (currentSV.getPreviousVersion() != null && !versions.contains(currentSV.getPreviousVersion())) {
                        versions.add(currentSV.getPreviousVersion());
                    }
                    versions.add(currentSV.getVersion());
                    ScenarioVersion finalCurrentSV = currentSV;
                    currentSV = scenariosMap
                            .get(sc)
                            .stream()
                            .filter(
                                    scenarioVersion ->
                                            scenarioVersion.getScenario().equals(sc) &&
                                            scenarioVersion.getPreviousVersion() != null &&
                                            scenarioVersion.getPreviousVersion().equals(finalCurrentSV.getVersion())
                            )
                            .findFirst()
                            .orElse(null);
                }

                computedScenarioList.add(new Scenario(sc, versions));
            });

            workspace.setSpace(new Space(
                    s,
                    computedScenarioList
            ));
        }

        return workspace;
    }
}
