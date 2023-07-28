package fr.vcity.sparqltosql.services;

import fr.vcity.sparqltosql.dto.RDFCompleteVersionedQuad;
import fr.vcity.sparqltosql.dto.VersionAncestry;
import fr.vcity.sparqltosql.repository.*;
import fr.vcity.sparqltosql.utils.SPARQLtoSQLVisitor;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpWalker;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class QuadQueryService implements IQuadQueryService {

    IRDFResourceOrLiteralRepository rdfResourceRepository;
    IRDFVersionedQuadRepository rdfVersionedQuadRepository;
    IRDFVersionedNamedGraphRepository rdfNamedGraphRepository;
    IVersionRepository versionRepository;
    RDFVersionedQuadComponent rdfVersionedQuadComponent;

    public QuadQueryService(
            IRDFResourceOrLiteralRepository rdfResourceRepository,
            IRDFVersionedQuadRepository rdfVersionedQuadRepository,
            IRDFVersionedNamedGraphRepository rdfNamedGraphRepository,
            IVersionRepository versionRepository,
            RDFVersionedQuadComponent rdfVersionedQuadComponent
    ) {
        this.rdfResourceRepository = rdfResourceRepository;
        this.rdfVersionedQuadRepository = rdfVersionedQuadRepository;
        this.rdfNamedGraphRepository = rdfNamedGraphRepository;
        this.versionRepository = versionRepository;
        this.rdfVersionedQuadComponent = rdfVersionedQuadComponent;
    }

    /**
     * @param requestedValidity the request validity
     * @return the quads list filtered by requestedValidity
     */
    @Override
    public List<RDFCompleteVersionedQuad> queryRequestedValidity(String requestedValidity) {
        log.debug("Requested: {}", requestedValidity);

        if (requestedValidity.equals("*")) {
            return rdfVersionedQuadComponent.findAll();
        }

        return rdfVersionedQuadComponent
                .findAllByValidity(requestedValidity);
    }

    /**
     * @param requestedVersion the request version number
     * @return the quads list filtered by requestedVersion where the fact has to be truthy
     */
    @Override
    public List<RDFCompleteVersionedQuad> queryRequestedVersion(Integer requestedVersion) {
        log.debug("Requested version: {}", requestedVersion);

        return rdfVersionedQuadComponent
                .findAllByVersion(requestedVersion);
    }

    /**
     * Parse the query String into an algebra
     *
     * @param queryString The given query string
     */
    @Override
    public List<RDFCompleteVersionedQuad> querySPARQL(String queryString) {
        getOperatorsFromQuery(queryString);
        return null;
    }

    /**
     * Returns the whole graph version of the database
     */
    @Override
    public List<VersionAncestry> getGraphVersion() {
        return versionRepository.getGraphVersion();
    }

    /**
     * Returns the hash of a specified version
     *
     * @param indexVersion The given index version
     */
    @Override
    public String getHashOfVersion(Integer indexVersion) {
        return versionRepository.getHashVersionByIndexVersion(indexVersion);
    }

    /**
     * Returns the algebra of the given query string
     *
     * @param queryString The query string
     */
    private void getOperatorsFromQuery(String queryString) {
        try {
            Query query = QueryFactory.create(queryString);
            switch (query.queryType()) {
                case SELECT -> {
                    Op op = Algebra.compile(query);
                    SPARQLtoSQLVisitor sparqLtoSQLVisitor = new SPARQLtoSQLVisitor();
                    OpWalker.walk(op, sparqLtoSQLVisitor);
                }
                case UNKNOWN, ASK, CONSTRUCT, DESCRIBE, CONSTRUCT_JSON ->
                        log.warn("Query with type: {} not implemented", query.queryType().toString());
            }
        } catch (QueryParseException e) {
            log.error(e.getMessage());
            log.warn("Query: {}", queryString);
            log.warn("Info: INSERT, UPDATE queries are not supported by the Query class");
            // TODO: implement ?
        }
    }
}
