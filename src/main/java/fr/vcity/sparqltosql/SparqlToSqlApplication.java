package fr.vcity.sparqltosql;

import fr.vcity.sparqltosql.services.VersioningSPARQLGSPRWProcessor;
import fr.vcity.sparqltosql.services.VersioningSPARQLQueryProcessor;
import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.fuseki.server.Operation;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SparqlToSqlApplication {

    public static void main(String[] args) {
        Dataset dataset = DatasetFactory.create();

        FusekiServer server = FusekiServer.create()
                .add("/rdf", dataset.asDatasetGraph())
                .registerOperation(Operation.Query, new VersioningSPARQLQueryProcessor())
                .registerOperation(Operation.GSP_RW, new VersioningSPARQLGSPRWProcessor())
                .build();
        server.start();
        SpringApplication.run(SparqlToSqlApplication.class, args);
    }
}
