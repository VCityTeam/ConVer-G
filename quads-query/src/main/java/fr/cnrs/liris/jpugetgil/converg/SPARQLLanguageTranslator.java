package fr.cnrs.liris.jpugetgil.converg;

import fr.cnrs.liris.jpugetgil.converg.entailment.EntailmentRegime;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SPARQLLanguageTranslator {

    private static final Logger log = LoggerFactory.getLogger(SPARQLLanguageTranslator.class);

    protected boolean condensedMode;

    protected EntailmentRegime entailmentRegime;

    protected SPARQLLanguageTranslator(boolean condensedMode, EntailmentRegime entailmentRegime) {
        this.condensedMode = condensedMode;
        this.entailmentRegime = entailmentRegime;

        if (condensedMode) {
            log.info("Condensed mode enabled");
        }
        if (entailmentRegime != EntailmentRegime.NONE) {
            log.info("Entailment regime: {}", entailmentRegime);
        }
    }

    abstract ResultSet translateAndExecSelect(Query sparqlQuery);

    abstract Dataset translateAndExecConstruct(Query query);
}
