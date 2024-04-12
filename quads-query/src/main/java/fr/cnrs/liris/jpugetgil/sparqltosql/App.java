package fr.cnrs.liris.jpugetgil.sparqltosql;

import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.fuseki.server.Operation;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.sparql.core.DatasetGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Query main class
 */
public class App {
    private static final Logger log = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        log.info("Building Fuseki server...");
        DatasetGraph dsg = DatasetFactory.createGeneral().asDatasetGraph();
        Dataset ds = DatasetFactory.wrap(dsg);

        FusekiServer server = FusekiServer.create()
                .port(8081)
                .add("/rdf", ds)
                .registerOperation(Operation.Query, new VersioningSPARQLQueryProcessor())
                // understand how to add static serving of fuseki ui files
                .build();
        log.info("Fuseki server is built, starting...");
        server.start();
    }
}
