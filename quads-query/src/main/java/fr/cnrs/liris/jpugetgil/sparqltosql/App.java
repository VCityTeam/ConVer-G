package fr.cnrs.liris.jpugetgil.sparqltosql;

import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.fuseki.server.Operation;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Query main class
 */
public class App {
    private static final Logger log = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        log.info("Building Fuseki server...");
        Dataset ds = DatasetFactory.createTxnMem();
        FusekiServer server = FusekiServer.create()
                .port(8081)
                .add("/rdf", ds.asDatasetGraph())
                .registerOperation(Operation.Query, new VersioningSPARQLQueryProcessor())
                .build();
        log.info("Fuseki server is built, starting...");
        server.start();
    }
}
