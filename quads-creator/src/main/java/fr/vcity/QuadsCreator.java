package fr.vcity;

import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFBase;
import org.apache.jena.sparql.core.Quad;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.OutputStream;

public class QuadsCreator {
    private static final Logger log = LoggerFactory.getLogger(QuadsCreator.class);

    public static void main(String[] args) {
        // Ensure we have the right number of arguments (input and output file)
        if (args.length != 2) {
            log.error("Usage: java QuadsCreator <inputFile> <outputFile>");
            System.exit(1);
        }

        // Input RDF file (containing triples)
        String inputFile = args[0];  // First argument: Input file
        String outputFile = args[1]; // Second argument: Output file (.trig)

        // Define a graph URI (can be any valid URI)
        String graphURI = "http://example.org/graph";

        // Step 1: Create an empty Dataset to store quads
        Dataset dataset = DatasetFactory.create();

        // Step 2: Create a StreamRDF to handle the conversion from triples to quads
        StreamRDF streamRDF = new StreamRDFBase() {
            @Override
            public void triple(Triple triple) {
                // Convert triple to quad by adding the graph name
                Quad quad = new Quad(NodeFactory.createURI(graphURI), triple);

                // Add the quad to the dataset
                dataset.asDatasetGraph().add(quad);
            }
        };

        // Step 3: Parse the input file and add the triples (as quads) to the dataset
        RDFDataMgr.parse(streamRDF, inputFile);

        // Step 4: Write the quads (from the dataset) into the output .trig file
        try (OutputStream outStream = new FileOutputStream(outputFile)) {
            // Save the dataset in TriG format
            RDFDataMgr.write(outStream, dataset, RDFFormat.TRIG_BLOCKS);
        } catch (Exception exception) {
            log.error(exception.getMessage());
            System.exit(1);
        }

        log.info("Quads saved to {}", outputFile);
    }
}