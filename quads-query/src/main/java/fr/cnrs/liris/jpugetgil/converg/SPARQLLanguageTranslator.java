package fr.cnrs.liris.jpugetgil.converg;

import fr.cnrs.liris.jpugetgil.converg.entailment.EntailmentRegime;
import fr.cnrs.liris.jpugetgil.converg.inference.InferenceConfig;
import fr.cnrs.liris.jpugetgil.converg.inference.InferenceRule;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public abstract class SPARQLLanguageTranslator {

    private static final Logger log = LoggerFactory.getLogger(SPARQLLanguageTranslator.class);

    protected boolean condensedMode;

    /** The inference sources to saturate over for this query (regime and/or SWRL). */
    protected InferenceConfig inferenceConfig;

    /** The regime component of {@link #inferenceConfig}, retained for SQLContext plumbing. */
    protected EntailmentRegime entailmentRegime;

    /** The server's verified SWRL rules, compiled for query-time saturation. */
    protected List<InferenceRule> swrlRules;

    protected SPARQLLanguageTranslator(boolean condensedMode, InferenceConfig inferenceConfig,
                                       List<InferenceRule> swrlRules) {
        this.condensedMode = condensedMode;
        this.inferenceConfig = inferenceConfig;
        this.entailmentRegime = inferenceConfig.regime();
        this.swrlRules = swrlRules == null ? List.of() : swrlRules;

        if (condensedMode) {
            log.info("Condensed mode enabled");
        }
        if (inferenceConfig.isEnabled()) {
            log.info("Query-time inference enabled ({})", inferenceConfig.describe());
        }
    }

    abstract ResultSet translateAndExecSelect(Query sparqlQuery);

    abstract Dataset translateAndExecConstruct(Query query);
}
