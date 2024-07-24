package fr.cnrs.liris.jpugetgil.converg;

import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.fuseki.main.sys.FusekiModules;
import org.apache.jena.fuseki.server.Operation;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.sparql.core.DatasetGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Query main class
 */
public class QuadsQueryApp {
    private static final Logger log = LoggerFactory.getLogger(QuadsQueryApp.class);

    public static void main(String[] args) {
        log.info("Building Fuseki server...");
        DatasetGraph dsg = DatasetFactory.createGeneral().asDatasetGraph();
        Dataset ds = DatasetFactory.wrap(dsg);
        FusekiModules modules = FusekiModules.create(new FusekiUI());
        FusekiServer server = FusekiServer.create()
                .port(8081)
                .enableCors(true, null)
                .add("/rdf", ds)
                .registerOperation(Operation.Query, new VersioningSPARQLQueryProcessor())
                .fusekiModules(modules)
                .build();
        log.info("Fuseki server is built, starting...");
        server.start();
    }
}
