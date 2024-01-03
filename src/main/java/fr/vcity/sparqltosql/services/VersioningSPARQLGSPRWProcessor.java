package fr.vcity.sparqltosql.services;

import lombok.extern.slf4j.Slf4j;
import org.apache.jena.fuseki.servlets.GSP_RW;
import org.apache.jena.fuseki.servlets.HttpAction;

@Slf4j
public class VersioningSPARQLGSPRWProcessor extends GSP_RW {
    /**
     * @param action
     */
    @Override
    protected void doPost(HttpAction action) {
        super.doPost(action);
    }
}
