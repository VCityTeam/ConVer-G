package fr.vcity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuadsCreator {
    private static final Logger log = LoggerFactory.getLogger(QuadsCreator.class);

    public static void main(String[] args) {
        // Ensure we have the right number of arguments
        if (args.length != 5) {
            log.error("Usage: java QuadsCreator <outputFolder> <inputFolder> <inputFile> <annotationType> <annotation>");
            System.exit(1);
        }

        String outputFolder = args[0];  // 1st argument: Output folder
        String inputFolder = args[1]; // 2nd argument: Input folder
        String inputFile = args[2]; // 3rd argument: Input file
        String annotationType = args[3]; // 4th argument: annotation type
        String annotation = args[4]; // 5th argument: annotation type

        new RDFConverter(annotationType, annotation, inputFolder, inputFile, outputFolder)
                .convert();

        log.info("Quads saved to {}", outputFolder);
    }
}