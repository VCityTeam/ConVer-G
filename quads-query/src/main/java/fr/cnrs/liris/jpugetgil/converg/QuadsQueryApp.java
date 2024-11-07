package fr.cnrs.liris.jpugetgil.converg;

import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.exporter.httpserver.HTTPServer;
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics;
import io.prometheus.metrics.model.snapshots.Unit;
import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.fuseki.main.sys.FusekiModules;
import org.apache.jena.fuseki.server.Operation;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.sparql.core.DatasetGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Query main class
 */
public class QuadsQueryApp {
    private static final Logger log = LoggerFactory.getLogger(QuadsQueryApp.class);

    public static void main(String[] args) throws InterruptedException, IOException {
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

        JvmMetrics.builder().register();

        HTTPServer exporterServer = HTTPServer.builder()
                .port(9400)
                .buildAndStart();

        Counter counter =
                Counter.builder()
                        .name("uptime_seconds_total")
                        .help("total number of seconds since this application was started")
                        .unit(Unit.SECONDS)
                        .register();

        System.out.println(
                "HTTPServer listening on port: " + exporterServer.getPort() + "/metrics");

        while (true) {
            Thread.sleep(1000);
            counter.inc();
        }
    }
}
