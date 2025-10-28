package fr.cnrs.liris.jpugetgil.converg;

import org.apache.jena.query.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Query CLI main class - uses Jena directly without the Fuseki layer
 */
public class QuadsQueryCLI {
    private static final Logger log = LoggerFactory.getLogger(QuadsQueryCLI.class);

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: QuadsQueryCLI <sparql-query-file>");
            System.err.println("  or: QuadsQueryCLI -q \"<inline-query>\"");
            System.exit(1);
        }

        String queryString;

        try {
            // Get query
            if (args[0].equals("-q") && args.length > 1) {
                queryString = args[1];
            } else {
                File queryFile = new File(args[0]);
                if (!queryFile.exists()) {
                    System.err.println("Query file not found: " + args[0]);
                    System.exit(1);
                }
                queryString = java.nio.file.Files.readString(queryFile.toPath());
            }

            log.info("Executing SPARQL query...");

            // Parse and execute query using VersioningQueryExecution
            Query query = QueryFactory.create(queryString);

            try (VersioningQueryExecution qExec = new VersioningQueryExecution(query)) {
                if (query.isSelectType()) {
                    ResultSet results = qExec.execSelect();
                    ResultSetFormatter.out(System.out, results, query);
                } else if (query.isConstructType()) {
                    org.apache.jena.rdf.model.Model model = qExec.execConstruct();
                    model.write(System.out, "TURTLE");
                } else if (query.isDescribeType()) {
                    org.apache.jena.rdf.model.Model model = qExec.execDescribe();
                    model.write(System.out, "TURTLE");
                } else if (query.isAskType()) {
                    boolean result = qExec.execAsk();
                    System.out.println(result);
                } else {
                    System.err.println("Unsupported query type");
                    System.exit(1);
                }
            }

            log.info("Query execution completed");

        } catch (Exception e) {
            log.error("Error executing query", e);
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
}
