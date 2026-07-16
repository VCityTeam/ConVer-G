package fr.cnrs.liris.jpugetgil.converg;

import fr.cnrs.liris.jpugetgil.converg.entailment.EntailmentRegime;
import fr.cnrs.liris.jpugetgil.converg.entailment.EntailmentRule;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public abstract class SPARQLLanguageTranslator {

    private static final Logger log = LoggerFactory.getLogger(SPARQLLanguageTranslator.class);

    protected boolean condensedMode;

    protected EntailmentRegime entailmentRegime;

    protected List<EntailmentRule> swrlRules;

    protected SPARQLLanguageTranslator(boolean condensedMode, EntailmentRegime entailmentRegime,
                                       List<EntailmentRule> swrlRules) {
        this.condensedMode = condensedMode;
        this.entailmentRegime = entailmentRegime;
        this.swrlRules = swrlRules;

        if (condensedMode) {
            log.info("Condensed mode enabled");
        }
        if (entailmentRegime != EntailmentRegime.NONE) {
            log.info("Entailment regime: {}", entailmentRegime);
        }
        if (!swrlRules.isEmpty()) {
            log.info("SWRL reasoning enabled with {} rule(s)", swrlRules.size());
        }
    }

    abstract ResultSet translateAndExecSelect(Query sparqlQuery);

    abstract Dataset translateAndExecConstruct(Query query);
}
