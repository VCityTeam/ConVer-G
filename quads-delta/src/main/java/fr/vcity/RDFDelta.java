package fr.vcity;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.sparql.core.Quad;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Computes the delta (additions and deletions) between two RDF files.
 */
public class RDFDelta {
    private static final Logger log = LoggerFactory.getLogger(RDFDelta.class);

    public static void main(String[] args) {
        if (args.length < 2) {
            log.error("Usage: java -cp <jar> fr.vcity.RDFDelta <file1> <file2> <outputDir>");
            System.exit(1);
        }
        String dataDir = "/data" + File.separator;
        String file1 = dataDir + args[0];
        String file2 = dataDir + args[1];

        Lang lang1 = validateAndGetLang(file1);
        Lang lang2 = validateAndGetLang(file2);
        validateCompatibleLangs(lang1, lang2);

        boolean isTriples = RDFLanguages.isTriples(lang1);
        String[] outFiles = getOutputFilenames(args[0], args[1], dataDir, isTriples);

        if (isTriples) {
            Model model1 = loadModel(file1);
            Model model2 = loadModel(file2);

            Model additions = model2.difference(model1);
            Model deletions = model1.difference(model2);

            writeModel(additions, outFiles[0]);
            writeModel(deletions, outFiles[1]);
        } else {
            Dataset dataset1 = loadDataset(file1);
            Dataset dataset2 = loadDataset(file2);

            Dataset additions = DatasetFactory.create();
            Dataset deletions = DatasetFactory.create();

            // Compute additions and deletions
            dataset2.asDatasetGraph().find().forEachRemaining((Quad quad) -> {
                if (!dataset1.asDatasetGraph().contains(quad)) {
                    additions.asDatasetGraph().add(quad);
                }
            });

            dataset1.asDatasetGraph().find().forEachRemaining((Quad quad) -> {
                if (!dataset2.asDatasetGraph().contains(quad)) {
                    deletions.asDatasetGraph().add(quad);
                }
            });

            writeDataset(additions, outFiles[0]);
            writeDataset(deletions, outFiles[1]);
        }

        log.info("Output written to " + outFiles[0] + " and " + outFiles[1]);
    }

    /**
     * Validates the RDF file and returns its language.
     */
    private static Lang validateAndGetLang(String filePath) {
        Lang lang = RDFLanguages.filenameToLang(filePath);
        if (lang == null || !RDFLanguages.isRegistered(lang)) {
            System.err.println("Unsupported RDF serialization: " + lang);
            System.exit(2);
        }
        return lang;
    }

    /**
     * Ensures both files use compatible RDF serialization (both triples or both quads).
     */
    private static void validateCompatibleLangs(Lang lang1, Lang lang2) {
        boolean isLang1Triples = RDFLanguages.isTriples(lang1);
        boolean isLang2Triples = RDFLanguages.isTriples(lang2);
        boolean bothTriples = isLang1Triples && isLang2Triples;
        boolean bothQuads = !isLang1Triples && !isLang2Triples;
        if (!bothTriples && !bothQuads) {
            System.err.println("Incompatible RDF serializations: " + lang1 + " and " + lang2);
            System.exit(2);
        }
    }

    /**
     * Loads an RDF model from a file.
     */
    private static Model loadModel(String filePath) {
        Model model = ModelFactory.createDefaultModel();
        try {
            RDFDataMgr.read(model, filePath);
        } catch (Exception e) {
            System.err.println("Error reading RDF file: " + filePath + ": " + e.getMessage());
            System.exit(3);
        }
        return model;
    }

    /**
     * Loads a RDF Dataset from a file (handling quads).
     */
    private static Dataset loadDataset(String filePath) {
        Dataset dataset = DatasetFactory.create();
        try {
            RDFDataMgr.read(dataset, filePath);
        } catch (Exception e) {
            System.err.println("Error reading RDF file: " + filePath + ": " + e.getMessage());
            System.exit(3);
        }
        return dataset;
    }

    /**
     * Returns output filenames for additions and deletions.
     */
    private static String[] getOutputFilenames(String arg1, String arg2, String dataDir, boolean isTriples) {
        String file1Base = new File(arg1).getName();
        String file2Base = new File(arg2).getName();
        int idx1 = file1Base.lastIndexOf('.');
        int idx2 = file2Base.lastIndexOf('.');
        if (idx1 > 0) file1Base = file1Base.substring(0, idx1);
        if (idx2 > 0) file2Base = file2Base.substring(0, idx2);
        String extension = isTriples ? ".ttl" : ".nq";
        String additionsOut = dataDir + file1Base + "-" + file2Base + ".additions" + extension;
        String deletionsOut = dataDir + file1Base + "-" + file2Base + ".deletions" + extension;
        return new String[] { additionsOut, deletionsOut };
    }

    /**
     * Writes a model to a file in the triples format.
     */
    private static void writeModel(Model model, String outFile) {
        try (FileOutputStream out = new FileOutputStream(outFile)) {
            model.write(out, "TTL");
        } catch (IOException e) {
            log.error("Error writing output file: " + outFile + ": " + e.getMessage());
            System.exit(4);
        }
    }

    /**
     * Writes a dataset to a file in the appropriate format.
     */
    private static void writeDataset(Dataset dataset, String outFile) {
        try (OutputStream outStream = new FileOutputStream(outFile)) {
            // Save the dataset in TriG format
            RDFDataMgr.write(outStream, dataset, RDFFormat.TRIG_BLOCKS);
        } catch (Exception exception) {
            log.error(exception.getMessage());
            System.exit(1);
        }
    }
}
