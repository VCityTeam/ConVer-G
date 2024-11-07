package fr.cnrs.liris.jpugetgil.converg;

import io.prometheus.metrics.core.metrics.Counter;
import org.apache.jena.atlas.json.JsonArray;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.ARQNotImplemented;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.util.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

public class VersioningQueryExecution implements QueryExecution {

    private static final Logger log = LoggerFactory.getLogger(VersioningQueryExecution.class);

    private final Query query;

    private final SPARQLLanguageTranslator translator;

    private final Counter selectQueryCounter = MetricsSingleton.getInstance().selectQueryCounter;

    private static final String TARGET_LANG = System.getenv("TARGET_LANG") == null ?
            "SQL" : getSupportedTargetLanguage(System.getenv("TARGET_LANG"));

    private static final boolean CONDENSED_MODE = System.getenv("CONDENSED_MODE") == null ||
            Boolean.parseBoolean(System.getenv("CONDENSED_MODE"));

    public VersioningQueryExecution(Query query) {
        this.query = query;
        this.translator = getTranslator();
    }

    private SPARQLLanguageTranslator getTranslator() {
        // Add switch case for other target languages when implemented
        log.info("Using target language: {}", TARGET_LANG);
        return new SPARQLtoSQLTranslator(CONDENSED_MODE);
    }

    private static String getSupportedTargetLanguage(String targetLang) {
        if (targetLang.equalsIgnoreCase("SQL")) {
            return "SQL";
        } else {
            log.error("Unsupported target language: {}", targetLang);
            throw new ARQNotImplemented("Unsupported target language: " + targetLang);
        }
    }

    @Override
    public Dataset getDataset() {
        return null;
    }

    @Override
    public Context getContext() {
        return Context.emptyContext();
    }

    @Override
    public Query getQuery() {
        return query;
    }

    @Override
    public String getQueryString() {
        return query.toString();
    }

    @Override
    public org.apache.jena.query.ResultSet execSelect() {
        // Increment the counter for the number of SELECT queries
        selectQueryCounter.inc();

        return translator.translateAndExecSelect(query);
    }

    @Override
    public Model execConstruct() {
        return null;
    }

    @Override
    public Model execConstruct(Model model) {
        return null;
    }

    @Override
    public Iterator<Triple> execConstructTriples() {
        return null;
    }

    @Override
    public Iterator<Quad> execConstructQuads() {
        return null;
    }

    @Override
    public Dataset execConstructDataset() {
        return translator.translateAndExecConstruct(query);
    }

    @Override
    public Dataset execConstructDataset(Dataset dataset) {
        return null;
    }

    @Override
    public Model execDescribe() {
        return null;
    }

    @Override
    public Model execDescribe(Model model) {
        return null;
    }

    @Override
    public Iterator<Triple> execDescribeTriples() {
        return null;
    }

    @Override
    public boolean execAsk() {
        return false;
    }

    @Override
    public JsonArray execJson() {
        return new JsonArray();
    }

    @Override
    public Iterator<JsonObject> execJsonItems() {
        return null;
    }

    @Override
    public void abort() {
        // Override engine execution, ignoring this method
    }

    @Override
    public void close() {
        // Override engine execution, ignoring this method
    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public long getTimeout1() {
        return 0;
    }

    @Override
    public long getTimeout2() {
        return 0;
    }
}
