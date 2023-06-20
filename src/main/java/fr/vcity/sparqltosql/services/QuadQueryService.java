package fr.vcity.sparqltosql.services;

import fr.vcity.sparqltosql.dao.RDFNamedGraph;
import fr.vcity.sparqltosql.dao.RDFResourceOrLiteral;
import fr.vcity.sparqltosql.dao.RDFVersionedQuad;
import fr.vcity.sparqltosql.dto.RDFCompleteVersionedQuad;
import fr.vcity.sparqltosql.repository.IRDFNamedGraphRepository;
import fr.vcity.sparqltosql.repository.IRDFResourceOrLiteralRepository;
import fr.vcity.sparqltosql.repository.IRDFVersionedQuadRepository;
import fr.vcity.sparqltosql.utils.SPARQLtoSQLVisitor;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.system.ErrorHandlerFactory;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpWalker;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class QuadQueryService implements IQuadQueryService {

    IRDFResourceOrLiteralRepository rdfResourceRepository;
    IRDFVersionedQuadRepository rdfVersionedQuadRepository;
    IRDFNamedGraphRepository rdfNamedGraphRepository;

    public QuadQueryService(
            IRDFResourceOrLiteralRepository rdfResourceRepository,
            IRDFVersionedQuadRepository rdfVersionedQuadRepository,
            IRDFNamedGraphRepository rdfNamedGraphRepository
    ) {
        this.rdfResourceRepository = rdfResourceRepository;
        this.rdfVersionedQuadRepository = rdfVersionedQuadRepository;
        this.rdfNamedGraphRepository = rdfNamedGraphRepository;
    }

    /**
     * @param requestedVersions the versions where the fact has to be truthy
     * @return the result of the given query
     */
    @Override
    public List<RDFCompleteVersionedQuad> query(String requestedVersions) {
        log.info("Requested: {}", requestedVersions);

        return rdfVersionedQuadRepository
                .findAllByValidityAndJoin(requestedVersions);
//                .parallelStream()
//                .map(rdfVersionedQuad ->
//                        new RDFCompleteVersionedQuad(
//                                rdfResourceRepository.findById(rdfVersionedQuad.getIdSubject()).orElse(null),
//                                rdfResourceRepository.findById(rdfVersionedQuad.getIdProperty()).orElse(null),
//                                rdfResourceRepository.findById(rdfVersionedQuad.getIdObject()).orElse(null),
//                                rdfNamedGraphRepository.findById(rdfVersionedQuad.getIdNamedGraph()).orElse(null),
//                                rdfVersionedQuad.getValidity()
//                        )
//                )
//                .collect(Collectors.toList());
    }

    /**
     * Parse the query String into an algebra
     *
     * @param queryString The given query string
     */
    public void query2(String queryString) {
        getOperatorsFromQuery(queryString);
    }

    /**
     * Import RDF statements represented in language <code>lang</code> to the model.
     * <br />Predefined values for <code>lang</code> are "TRIG" and "NQUADS"
     *
     * @param modelString The input model string
     * @param lang        The input model language
     */
    public void importModel(String modelString, String lang) {
        Integer length = rdfVersionedQuadRepository.createNewVersion();

        try (InputStream in = new ByteArrayInputStream(modelString.getBytes())) {
            Dataset dataset =
                    RDFParser.create()
                            .source(in)
                            .lang(RDFLanguages.nameToLang(lang))
                            .errorHandler(ErrorHandlerFactory.errorHandlerStrict)
                            .toDataset();

            for (Iterator<Resource> i = dataset.listModelNames(); i.hasNext(); ) {
                Resource namedModel = i.next();
                Model model = dataset.getNamedModel(namedModel);
                log.info("Name Graph : {}", namedModel.getURI());

                for (StmtIterator s = model.listStatements(); s.hasNext(); ) {
                    Statement statement = s.nextStatement();
                    RDFNode subject = statement.getSubject();
                    RDFNode predicate = statement.getPredicate();
                    RDFNode object = statement.getObject();

                    RDFResourceOrLiteral savedRDFSubject = saveRDFResourceOrLiteralOrReturnExisting(subject, "Subject");
                    RDFResourceOrLiteral savedRDFPredicate = saveRDFResourceOrLiteralOrReturnExisting(predicate, "Predicate");
                    RDFResourceOrLiteral savedRDFObject = saveRDFResourceOrLiteralOrReturnExisting(object, "Object");
                    RDFNamedGraph savedRDFNamedGraph = saveRDFNamedGraphOrReturnExisting(namedModel.getURI());

                    Optional<RDFVersionedQuad> optionalRDFVersionedQuad =
                            rdfVersionedQuadRepository.findByIdSubjectAndIdPropertyAndIdObjectAndIdNamedGraph(
                                    savedRDFSubject.getId(),
                                    savedRDFPredicate.getId(),
                                    savedRDFObject.getId(),
                                    savedRDFNamedGraph.getId()
                            );

                    RDFVersionedQuad rdfVersionedQuad;

                    if (optionalRDFVersionedQuad.isPresent()) {
                        log.info("Updated quad (NG: {}, S: {}, P: {}, O: {})",
                                namedModel.getURI(),
                                savedRDFSubject.getName(),
                                savedRDFPredicate.getName(),
                                savedRDFObject.getName()
                        );

                        rdfVersionedQuad = optionalRDFVersionedQuad.get();
                        String bitSetString = rdfVersionedQuad.getValidity();
                        StringBuilder string = new StringBuilder(bitSetString);
                        string.setCharAt(bitSetString.length() - 1, '1');
                        rdfVersionedQuad.setValidity(string.toString());

                    } else {
                        log.info("Insert quad (NG: {}, S: {}, P: {}, O: {})",
                                namedModel.getURI(),
                                savedRDFSubject.getName(),
                                savedRDFPredicate.getName(),
                                savedRDFObject.getName()
                        );

                        StringBuilder string = new StringBuilder("0".repeat(length == null ? 1 : length));
                        string.setCharAt(length == null ? 0 : length - 1, '1');

                        rdfVersionedQuad = new RDFVersionedQuad(
                                savedRDFSubject.getId(),
                                savedRDFPredicate.getId(),
                                savedRDFObject.getId(),
                                savedRDFNamedGraph.getId(),
                                string.toString()
                        );
                    }
                    rdfVersionedQuadRepository.save(rdfVersionedQuad);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     *  Deletes all the elements inside the database
     */
    public void resetDatabase() {
        rdfVersionedQuadRepository.deleteAll();
        rdfResourceRepository.deleteAll();
        rdfNamedGraphRepository.deleteAll();
    }

    /**
     * Saves and return the RDF Named Graph inside the database if it doesn't exist, else returns the existing one.
     *
     * @param uri The RDF named graph URI
     * @return The saved or existing RDFNamedGraph element
     */
    private RDFNamedGraph saveRDFNamedGraphOrReturnExisting(String uri) {
        Optional<RDFNamedGraph> optionalRDFNamedGraph = rdfNamedGraphRepository.findByName(uri);

        if (optionalRDFNamedGraph.isPresent()) {
            log.info("Found named graph: {}", uri);

            return optionalRDFNamedGraph.get();
        }

        log.info("Insert named graph: {}", uri);
        RDFNamedGraph rdfNamedGraph = new RDFNamedGraph(uri);
        return rdfNamedGraphRepository.save(rdfNamedGraph);
    }

    /**
     * Saves and return the RDF node inside the database if it doesn't exist, else returns the existing one.
     *
     * @param spo  The RDF node
     * @param type The RDF node type (logging purpose)
     * @return The saved or existing RDFResourceOrLiteral element
     */
    private RDFResourceOrLiteral saveRDFResourceOrLiteralOrReturnExisting(RDFNode spo, String type) {
        RDFResourceOrLiteral rdfResourceOrLiteral;
        if (spo.isLiteral()) {
            Literal literal = spo.asLiteral();
            String literalValue = literal.getString();

            Optional<RDFResourceOrLiteral> optionalRDFResourceOrLiteral =
                    rdfResourceRepository.findByNameAndType(literalValue, literal.getDatatype().toString());

            if (optionalRDFResourceOrLiteral.isPresent()) {
                log.info("Found {} literal: {}", type, literalValue);

                return optionalRDFResourceOrLiteral.get();
            }

            log.info("Insert {} resource: {}", type, literalValue);
            rdfResourceOrLiteral = new RDFResourceOrLiteral(literalValue, spo.asLiteral().getDatatype().toString());

        } else {

            // Get element if exists or save new
            Optional<RDFResourceOrLiteral> optionalRDFResourceOrLiteral =
                    rdfResourceRepository.findByNameAndType(spo.toString(), null);

            if (optionalRDFResourceOrLiteral.isPresent()) {
                log.info("Found {} resource: {}", type, spo);

                return optionalRDFResourceOrLiteral.get();
            }

            log.info("Insert {} resource: {}", type, spo);
            rdfResourceOrLiteral = new RDFResourceOrLiteral(spo.toString(), null);
        }

        return rdfResourceRepository.save(rdfResourceOrLiteral);
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
