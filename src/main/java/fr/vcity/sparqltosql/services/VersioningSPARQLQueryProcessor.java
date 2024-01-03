package fr.vcity.sparqltosql.services;

import lombok.extern.slf4j.Slf4j;
import org.apache.jena.atlas.lib.Pair;
import org.apache.jena.fuseki.servlets.HttpAction;
import org.apache.jena.fuseki.servlets.SPARQLQueryProcessor;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.exec.QueryExec;
import org.apache.jena.sparql.exec.QueryExecDatasetBuilder;
import org.apache.jena.sparql.exec.QueryExecutionAdapter;

@Slf4j
public class VersioningSPARQLQueryProcessor extends SPARQLQueryProcessor {

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
