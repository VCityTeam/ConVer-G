package fr.cnrs.liris.jpugetgil.converg;

import fr.cnrs.liris.jpugetgil.converg.entailment.EntailmentRegime;
import fr.cnrs.liris.jpugetgil.converg.inference.InferenceConfig;
import fr.cnrs.liris.jpugetgil.converg.swrl.SWRLReasoner;
import io.prometheus.metrics.core.metrics.Counter;
import org.apache.jena.atlas.json.JsonArray;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.sparql.ARQNotImplemented;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.util.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

public class VersioningQueryExecution implements QueryExecution {

    private static final Logger log = LoggerFactory.getLogger(VersioningQueryExecution.class);

    private final Query query;

    private final SPARQLLanguageTranslator translator;

    private final Counter selectQueryCounter = MetricsSingleton.getInstance().queryCounter;

    private static final String TARGET_LANG = System.getenv("TARGET_LANG") == null ?
            "SQL" : getSupportedTargetLanguage(System.getenv("TARGET_LANG"));

    private static final boolean CONDENSED_MODE = System.getenv("CONDENSED_MODE") == null ||
            Boolean.parseBoolean(System.getenv("CONDENSED_MODE"));

    private static final EntailmentRegime ENTAILMENT_REGIME =
            EntailmentRegime.fromString(System.getenv("ENTAILMENT_REGIME"));

    private static final SWRLReasoner SWRL_REASONER = SWRLReasoner.fromEnv();

    /** The inference applied when a query does not specify {@code ?infer}. */
    private static final InferenceConfig SERVER_DEFAULT =
            new InferenceConfig(ENTAILMENT_REGIME, SWRL_REASONER.isEnabled());

    /**
     * @param query      the SPARQL query
     * @param inferParam the raw {@code ?infer=} request parameter (may be null), used to
     *                   enable/disable reasoning per query
     */
    public VersioningQueryExecution(Query query, String inferParam) {
        this.query = query;
        InferenceConfig config = InferenceConfig.resolve(inferParam, SERVER_DEFAULT, SWRL_REASONER.isEnabled());
        this.translator = getTranslator(config);
    }

    private SPARQLLanguageTranslator getTranslator(InferenceConfig config) {
        // Add switch case for other target languages when implemented
        log.info("Using target language: {}", TARGET_LANG);
        return new SPARQLtoSQLTranslator(CONDENSED_MODE, config, SWRL_REASONER.getRules());
    }

    /**
     * Describes the server's default inference configuration, e.g. "RDFS", "SWRL" or
     * "RDFS+SWRL". Empty when no inference is enabled by default. Individual queries may
     * override it with {@code ?infer=}.
     */
    public static String inferenceMode() {
        return SERVER_DEFAULT.describe();
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
        return execConstruct(ModelFactory.createDefaultModel());
    }

    @Override
    public Model execConstruct(Model model) {
        execConstructQuads().forEachRemaining(quad -> model.getGraph().add(quad.asTriple()));
        return model;
    }

    @Override
    public Iterator<Triple> execConstructTriples() {
        return execConstruct().getGraph().find();
    }

    @Override
    public Iterator<Quad> execConstructQuads() {
        return translator.translateAndExecConstruct(query).asDatasetGraph().find();
    }

    @Override
    public Dataset execConstructDataset() {
        return translator.translateAndExecConstruct(query);
    }

    @Override
    public Dataset execConstructDataset(Dataset dataset) {
        DatasetGraph dsg = dataset.asDatasetGraph();
        execConstructQuads().forEachRemaining(dsg::add);
        return dataset;
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
