package fr.cnrs.liris.jpugetgil.converg;

import org.apache.jena.atlas.lib.Pair;
import org.apache.jena.fuseki.servlets.HttpAction;
import org.apache.jena.fuseki.servlets.SPARQLQueryProcessor;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.exec.QueryExec;
import org.apache.jena.sparql.exec.QueryExecAdapter;
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
        if (log.isDebugEnabled()) {
            log.debug("Validating action {}, with context {}", action, action.getContext());
        }
    }

    /**
     * @param action
     * @param query
     */
    @Override
    protected void validateQuery(HttpAction action, Query query) {
        if (log.isDebugEnabled()) {
            log.debug("Validating query {} and action {}", query, action);
        }
    }

    /**
     * @param action
     * @param query
     * @param s
     * @return
     */
    @Override
    protected Pair<DatasetGraph, Query> decideDataset(HttpAction action, Query query, String s) {
        DatasetGraph dsg = action.getActiveDSG();
        return new Pair<>(dsg, query);
    }

    @Override
    protected QueryExec createQueryExec(HttpAction action, Query query, DatasetGraph dataset) {
        return QueryExecAdapter.adapt(new VersioningQueryExecution(query));
    }
}
