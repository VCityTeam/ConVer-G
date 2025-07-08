package fr.cnrs.liris.jpugetgil.converg;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SPARQLLanguageTranslator {

    private static final Logger log = LoggerFactory.getLogger(SPARQLLanguageTranslator.class);

    protected boolean condensedMode;

    protected SPARQLLanguageTranslator(boolean condensedMode) {
        this.condensedMode = condensedMode;

        if (condensedMode) {
            log.info("Condensed mode enabled");
        }
    }

    abstract ResultSet translateAndExecSelect(Query sparqlQuery);

    abstract Dataset translateAndExecConstruct(Query query);
}
