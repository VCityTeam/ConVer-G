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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class RDFConverter {
    private static final Logger log = LoggerFactory.getLogger(RDFConverter.class);

    private final Dataset dataset;
    private final Dataset metadataDataset;
    private final String annotationType;
    private final String annotation;
    private final String inputFolder;
    private final String inputFile;
    private final String outputFolder;

    /**
     * Constructor of RDFConverter
     *
     * @param annotationType The type of annotation (relational or theoretical)
     * @param annotation     The annotation (quad)
     * @param inputFolder    The folder containing the input files
     * @param inputFile      The file to be converted
     * @param outputFolder   The folder to write the output to
     */
    public RDFConverter(String annotationType, String annotation, String inputFolder, String inputFile, String outputFolder) {
        this.dataset = DatasetFactory.create();
        this.metadataDataset = DatasetFactory.create();
        this.annotationType = annotationType;
        this.annotation = annotation;
        this.inputFolder = inputFolder;
        this.inputFile = inputFile;
        this.outputFolder = outputFolder;
    }

    /**
     * It takes an input file, an input format, an output file, and an annotation, and it adds annotation to the
     * input file from the input format and saves it to the output file
     */
    public void convert() {
        log.info("({} annotation) - file: {} with {}", this.annotationType, this.inputFile, this.annotation);

        insertQuadsToDataset();

        if (annotationType.equals(AnnotationType.THEORETICAL.getLabel())) {
            insertQuadsToMetadataDataset();
        }
    }

    private void insertQuadsToDataset() {
        String inputFileName = getInputFileName(inputFolder, inputFile);
        String outputFile = getOutputFileName(outputFolder, inputFile);
        String graphURI = getGraphNameURI(inputFile);

        getStreamRDF(inputFileName, outputFile, graphURI)
                .finish();
    }

    private void insertQuadsToMetadataDataset() {
        String inputFileName = getInputFileName(inputFolder, inputFile);
        String outputFile = getOutputFileName(outputFolder, inputFile);
        String graphURI = getGraphNameURI(inputFile);

        getStreamRDF(inputFileName, outputFile, graphURI)
                .finish();

        getMetadataStreamRDF(inputFileName, graphURI)
                .finish();
    }

    /**
     * It reads a file and converts it into a stream of quads and triple
     *
     * @param inputFileName The input file name
     * @param graphURI      The graph name
     * @return A stream of quads
     */
    private StreamRDF getMetadataStreamRDF(String inputFileName, String graphURI) {
        StreamRDF quadStreamRDF = new StreamRDFBase() {
            @Override
            public void quad(Quad quad) {
                metadataDataset.asDatasetGraph().add(quad);
            }
        };

        metadataDataset
                .asDatasetGraph()
                .add(new Quad(NodeFactory.createURI("https://github.com/VCityTeam/ConVer-G/Named-Graph#Metadata"),
                        Triple.create(
                                NodeFactory.createURI(graphURI),
                                NodeFactory.createURI("https://github.com/VCityTeam/ConVer-G/Version#is-version-of"),
                                NodeFactory.createURI(getAnnotationURI(annotation))
                        )));

        metadataDataset
                .asDatasetGraph()
                .add(new Quad(NodeFactory.createURI("https://github.com/VCityTeam/ConVer-G/Named-Graph#Metadata"),
                        Triple.create(
                                NodeFactory.createURI(graphURI),
                                NodeFactory.createURI("https://github.com/VCityTeam/ConVer-G/Version#is-in-version"),
                                NodeFactory.createURI(getVersionURI(inputFileName))
                        )));

        File file = new File(getTheoreticalFileName(outputFolder));
        if (!file.exists()) {
            try {
                // Create an empty file
                if (file.createNewFile()) {
                    log.info("File created: {}", getTheoreticalFileName(outputFolder));
                } else {
                    log.info("File could not be created.");
                }
            } catch (IOException e) {
                log.info("An error occurred while creating the file.");
                throw new RuntimeException();
            }
        }

        RDFDataMgr.parse(quadStreamRDF, getTheoreticalFileName(outputFolder));

        try (OutputStream outStream = new FileOutputStream(getTheoreticalFileName(outputFolder))) {
            // Save the dataset in TriG format
            RDFDataMgr.write(outStream, metadataDataset, RDFFormat.TRIG_BLOCKS);
        } catch (Exception exception) {
            log.error(exception.getMessage());
            System.exit(1);
        }

        return quadStreamRDF;
    }

    /**
     * It reads a file and converts it into a stream of triples
     *
     * @param inputFileName The input file name
     * @param outputFile    The output file name
     * @param graphURI      The graph name
     * @return A stream of triples
     */
    private StreamRDF getStreamRDF(String inputFileName, String outputFile, String graphURI) {
        StreamRDF streamRDF = new StreamRDFBase() {
            @Override
            public void triple(Triple triple) {
                // Convert triple to quad by adding the graph name
                Quad quad = new Quad(NodeFactory.createURI(graphURI), triple);

                // Add the quad to the dataset
                dataset.asDatasetGraph().add(quad);
            }
        };

        RDFDataMgr.parse(streamRDF, inputFileName);

        try (OutputStream outStream = new FileOutputStream(outputFile)) {
            // Save the dataset in TriG format
            RDFDataMgr.write(outStream, dataset, RDFFormat.TRIG_BLOCKS);
        } catch (Exception exception) {
            log.error(exception.getMessage());
            System.exit(1);
        }

        return streamRDF;
    }

    /**
     * This function generates the input file name
     *
     * @param inputFolder The folder containing the input files
     * @param inputFile   The input file name
     * @return The built input file
     */
    private String getInputFileName(String inputFolder, String inputFile) {
        return new File(inputFolder, inputFile).getPath();
    }

    /**
     * This function generates the theoretical file name
     *
     * @param inputFolder The folder containing the input files
     * @return The built theoretical
     */
    private String getTheoreticalFileName(String inputFolder) {
        return getInputFileName(inputFolder, "theoretical_annotations.trig");
    }

    /**
     * This function generates the output file name
     *
     * @param inputFolder The folder containing the input files
     * @param inputFile   The input file name
     * @return The built output file name
     */
    private String getOutputFileName(String inputFolder, String inputFile) {
        if (annotationType.equals(AnnotationType.THEORETICAL.getLabel())) {
            return getInputFileName(inputFolder, inputFile) + ".theoretical.trig";
        } else {
            return getInputFileName(inputFolder, inputFile) + ".relational.trig";
        }
    }

    /**
     * This function generates the version URI
     *
     * @param inputFileName The input file name
     * @return The version
     */
    private String getVersionURI(String inputFileName) {
        return "https://github.com/VCityTeam/ConVer-G/Version#" + inputFileName;
    }

    /**
     * This function generates the annotation URI
     *
     * @param annotation The annotation (graph name)
     * @return The URI of the annotation
     */
    private String getAnnotationURI(String annotation) {
        return "https://github.com/VCityTeam/ConVer-G/Named-Graph#" + annotation;
    }

    /**
     * It generates a unique URI for the graph name given a seed (input file)
     *
     * @param inputFile The seed to generate the URI
     * @return The URI of the graph name
     */
    private String getGraphNameURI(String inputFile) {
        if (annotationType.equals(AnnotationType.THEORETICAL.getLabel())) {
            try {
                String toHash = annotation + inputFile;
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hashBytes = digest.digest(toHash.getBytes(StandardCharsets.UTF_8));

                // Convert byte array to hex string
                StringBuilder hexString = new StringBuilder();
                for (byte b : hashBytes) {
                    String hex = Integer.toHexString(0xff & b);
                    if (hex.length() == 1) hexString.append('0');
                    hexString.append(hex);
                }

                return "https://github.com/VCityTeam/ConVer-G/Versioned-Named-Graph#" + hexString;
            } catch (NoSuchAlgorithmException e) {
                log.error(e.getMessage());
                throw new RuntimeException(e.getMessage());
            }
        } else {
            return "https://github.com/VCityTeam/ConVer-G/Named-Graph#" + annotation;
        }
    }
}
