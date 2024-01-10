package fr.cnrs.liris.jpugetgil.sparqltosql;

import org.apache.jena.atlas.lib.Pair;
import org.apache.jena.fuseki.servlets.HttpAction;
import org.apache.jena.fuseki.servlets.SPARQLQueryProcessor;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.sparql.core.DatasetGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VersioningSPARQLQueryProcessor extends SPARQLQueryProcessor {

    private static final Logger log = LoggerFactory.getLogger(VersioningSPARQLQueryProcessor.class);

    /**
     * @param action
     */
    @Override
    protected void validateRequest(HttpAction action) {
        log.info("Validating action {}, with context {}", action, action.getContext());
    }

    /**
     * @param action
     * @param query
     */
    @Override
    protected void validateQuery(HttpAction action, Query query) {
        log.info("Validating query {} in action {}", query, action);
    }

    /**
     * @param action
     * @param query
     * @param s
     * @return
     */
    @Override
    protected Pair<DatasetGraph, Query> decideDataset(HttpAction action, Query query, String s) {
        log.info("Creating dummy dataset for action {}, query {}, and s {}", action, query, s);
        DatasetGraph dsg = action.getActiveDSG();
        return new Pair<>(dsg, query);
    }

    @Override
    protected QueryExecution createQueryExecution(HttpAction action, Query query, DatasetGraph dataset) {
        log.info("Creating queryExecution for action {}, query {}, and dataset {}", action, query, dataset);
        return new VersioningQueryExecution(query);
    }
}
