package fr.vcity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

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

        File outputF = new File(outputFolder);

        // Create the directory if it does not exist
        if (outputF.mkdirs()) {
            log.info("The folder ({}) was created successfully", outputF.getAbsolutePath());
        }

        if (inputFile.equals("*")) {
            File folder = new File(inputFolder);

            if (folder.exists() && folder.isDirectory()) {
                // Get the list of all files and directories in the folder
                File[] filesList = folder.listFiles();

                // Check if the directory is empty
                if (filesList != null) {
                    for (File file : filesList) {
                        if (file.isFile()) {
                            new RDFConverter(annotationType, annotation, inputFolder, file.getName(), outputFolder)
                                    .convert();
                        }
                    }
                }
            } else {
                log.info("The specified folder ({}) does not exist or is not a directory.", folder.getAbsolutePath());
            }
        } else {
            new RDFConverter(annotationType, annotation, inputFolder, inputFile, outputFolder)
                    .convert();
        }

        log.info("Quads saved to {}", outputF.getAbsolutePath());
    }
}