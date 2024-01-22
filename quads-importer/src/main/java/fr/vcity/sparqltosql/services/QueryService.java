package fr.vcity.sparqltosql.services;

import fr.vcity.sparqltosql.dao.ScenarioVersion;
import fr.vcity.sparqltosql.dto.CompleteVersionedQuad;
import fr.vcity.sparqltosql.dto.Scenario;
import fr.vcity.sparqltosql.dto.Space;
import fr.vcity.sparqltosql.dto.Workspace;
import fr.vcity.sparqltosql.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.query.Syntax;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.op.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class QueryService implements IQueryService {

    IResourceOrLiteralRepository rdfResourceRepository;
    IVersionedQuadRepository rdfVersionedQuadRepository;
    IVersionedNamedGraphRepository rdfNamedGraphRepository;
    IVersionedWorkspaceRepository versionedWorkspaceRepository;
    IVersionRepository versionRepository;
    VersionedQuadComponent versionedQuadComponent;

    public QueryService(
            IResourceOrLiteralRepository rdfResourceRepository,
            IVersionedQuadRepository rdfVersionedQuadRepository,
            IVersionedNamedGraphRepository rdfNamedGraphRepository,
            IVersionRepository versionRepository,
            VersionedQuadComponent versionedQuadComponent,
            IVersionedWorkspaceRepository versionedWorkspaceRepository
    ) {
        this.rdfResourceRepository = rdfResourceRepository;
        this.rdfVersionedQuadRepository = rdfVersionedQuadRepository;
        this.rdfNamedGraphRepository = rdfNamedGraphRepository;
        this.versionRepository = versionRepository;
        this.versionedQuadComponent = versionedQuadComponent;
        this.versionedWorkspaceRepository = versionedWorkspaceRepository;
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
     * Parse the query String into an algebra
     *
     * @param queryString The given query string
     */
    @Override
    public List<CompleteVersionedQuad> querySPARQL(String queryString) {
        getOperatorsFromQuery(queryString);
        return Collections.emptyList();
    }

    /**
     * Returns the result of the query
     *
     * @param query The given query
     */
    @Override
    public List<CompleteVersionedQuad> getOperatorsFromQueryARQ(Query query) {
        try {
            switch (query.queryType()) {
                case SELECT -> {
                    log.info("******* Op walker - OpVisitor *******");
                    Op op = Algebra.compile(query);
                    buildSelectSQL(op, null);
                }
                case ASK, CONSTRUCT, DESCRIBE, CONSTRUCT_JSON ->
                        log.warn("Query with type: {} not implemented", query.queryType().toString());
                case UNKNOWN -> log.warn("Unknown query not supported. Ignoring it.");
            }
        } catch (QueryParseException e) {
            log.error(e.getMessage());
            log.warn("Query: {}", query);
            log.warn("Info: INSERT, UPDATE queries are not supported by the Query class");
        }

        return Collections.emptyList();
    }

    /**
     * Returns the whole graph version of the database
     */
    @Override
    public Workspace getGraphVersion() {
        List<ScenarioVersion> scenarios = versionedWorkspaceRepository.getAllScenarios();

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

    /**
     * Returns the algebra of the given query string
     *
     * @param queryString The query string
     */
    private void getOperatorsFromQuery(String queryString) {
        try {
            Query query = QueryFactory.create(queryString, Syntax.syntaxSPARQL);
            switch (query.queryType()) {
                case SELECT -> {
                    log.info("******* Op walker - OpVisitor *******");
                    Op op = Algebra.compile(query);
                    buildSelectSQL(op, null);
                }
                case ASK, CONSTRUCT, DESCRIBE, CONSTRUCT_JSON ->
                        log.warn("Query with type: {} not implemented", query.queryType().toString());
                case UNKNOWN -> log.warn("Unknown query not supported. Ignoring it.");
            }
        } catch (QueryParseException e) {
            log.error(e.getMessage());
            log.warn("Query: {}", queryString);
            log.warn("Info: INSERT, UPDATE queries are not supported by the Query class");
        }
    }

    /**
     * Use pattern matching to analyse the SPARQL query and builds the SQL query
     *
     * @param op The algebra operator
     */
    private String buildSelectSQL(Op op, Op context /* Construire une classe Context */) {
        if (context != null) {
            log.info("Context : {}",  context);
        }
        return switch (op) {
            case OpJoin opJoin -> {
                String left = buildSelectSQL(opJoin.getLeft(), opJoin);
                String right = buildSelectSQL(opJoin.getRight(), opJoin);
                yield "SELECT * FROM " + left + " INNER JOIN " + right + " ON " + left + ".id = " + right + ".id";
            }
            case OpLeftJoin opLeftJoin -> {
                String left = buildSelectSQL(opLeftJoin.getLeft(), opLeftJoin);
                String right = buildSelectSQL(opLeftJoin.getRight(), opLeftJoin);
                yield "SELECT * FROM " + left + " LEFT JOIN " + right + " ON " + left + ".id = " + right + ".id";
            }
            case OpUnion opUnion -> {
                String left = buildSelectSQL(opUnion.getLeft(), opUnion);
                String right = buildSelectSQL(opUnion.getRight(), opUnion);
                yield "SELECT * FROM " + left + " UNION " + right;
            }
            case OpProject opProject -> {
                String subOp = buildSelectSQL(opProject.getSubOp(), opProject);
                yield "SELECT * FROM " + subOp;
            }
            case OpTable opTable -> {
                String tableName = opTable.getTable().toString();
                yield "SELECT * FROM " + tableName;
            }
            case OpQuadPattern opQuadPattern -> {
                String graph = opQuadPattern.getGraphNode().getURI();
//                String subject =
//                String predicate =
//                String object =
//                yield "SELECT * FROM " + graph + " WHERE " + subject + " " + predicate + " " + object;
                yield "SELECT * FROM " + graph;
            }
            case OpExtend opExtend -> {
                String subOp = buildSelectSQL(opExtend.getSubOp(), opExtend);
                yield "SELECT * FROM " + subOp;
            }
            case OpDistinct opDistinct -> {
                String subOp = buildSelectSQL(opDistinct.getSubOp(), opDistinct);
                yield "SELECT * FROM " + subOp;
            }
            case OpFilter opFilter -> {
                String subOp = buildSelectSQL(opFilter.getSubOp(), opFilter);
                yield "SELECT * FROM " + subOp;
            }
            case OpOrder opOrder -> {
                String subOp = buildSelectSQL(opOrder.getSubOp(), opOrder);
                yield "SELECT * FROM " + subOp;
            }
            case OpGroup opGroup -> {
                String subOp = buildSelectSQL(opGroup.getSubOp(), opGroup);
                yield "SELECT * FROM " + subOp;
            }
            case OpSlice opSlice -> {
                String subOp = buildSelectSQL(opSlice.getSubOp(), opSlice);
                yield "SELECT * FROM " + subOp;
            }
            case OpTopN opTopN -> {
                String subOp = buildSelectSQL(opTopN.getSubOp(), opTopN);
                yield "SELECT * FROM " + subOp;
            }
            case OpPath opPath -> {
                // TODO
//                yield "SELECT * FROM " + subOp;
                yield "TODO";
            }
            case OpLabel opLabel -> {
                String subOp = buildSelectSQL(opLabel.getSubOp(), opLabel);
                yield "SELECT * FROM " + subOp;
            }
            case OpNull opNull -> {
//                String subOp = buildSelectSQL();
//                yield "SELECT * FROM " + subOp;
                yield "TODO";
            }
            case OpList opList -> {
                String subOp = buildSelectSQL(opList.getSubOp(), opList);
                yield "SELECT * FROM " + subOp;
            }
            case OpBGP opBGP -> {
//                String subOp = buildSelectSQL(opBGP.);
//                yield "SELECT * FROM " + subOp;
                yield "TODO";
            }
            case OpGraph opGraph -> {
                String subOp = buildSelectSQL(opGraph.getSubOp(), opGraph);
                yield "SELECT * FROM " + subOp;
            }
            case OpTriple opTriple -> {
                String subject = opTriple.getTriple().getSubject().toString();
                String predicate = opTriple.getTriple().getPredicate().toString();
                String object = opTriple.getTriple().getObject().toString();
                yield "SELECT * FROM ... " + subject + " " + predicate + " " + object;
            }
            default -> throw new IllegalStateException("Unexpected value: " + op.getName());
        };
    }
}
