package fr.vcity.converg.commands;

import fr.vcity.converg.services.QuadImportService;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.riot.RiotException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.io.File;
import java.io.IOException;

@ShellComponent
@Slf4j
public class QuadImportCommands {

    @Value("${quad.importer.enabled:true}")
    boolean quadImporterEnabled;

    private final QuadImportService quadImportService;

    public QuadImportCommands(QuadImportService quadImportService) {
        this.quadImportService = quadImportService;
    }

    @ShellMethod(key = "import-version", value = "Import quads from a file and create a new version")
    public String importVersion(@ShellOption(help = "Path to the file containing quads/triples") String filePath) {
        if (!quadImporterEnabled) {
            return "Quad importer is disabled";
        }

        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            return "Error: File not found or is not a valid file: " + filePath;
        }

        try {
            Integer versionIndex = quadImportService.importModel(file);
            return "Successfully imported model. Created version index: " + versionIndex;
        } catch (RiotException e) {
            log.error("Error importing model", e);
            return "Error: Invalid RDF content - " + e.getMessage();
        }
    }

    @ShellMethod(key = "flatten", value = "Flatten all versioned quads")
    public String flattenVersionedQuads() {
        if (!quadImporterEnabled) {
            return "Quad importer is disabled";
        }

        try {
            quadImportService.flattenVersionedQuads();
            return "Successfully flattened all versioned quads";
        } catch (Exception e) {
            log.error("Error flattening versioned quads", e);
            return "Error: " + e.getMessage();
        }
    }

    @ShellMethod(key = "import-metadata", value = "Import metadata triples from a file")
    public String importMetadata(@ShellOption(help = "Path to the file containing metadata triples") String filePath) {
        if (!quadImporterEnabled) {
            return "Quad importer is disabled";
        }

        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            return "Error: File not found or is not a valid file: " + filePath;
        }

        try {
            quadImportService.importMetadata(file);
            return "Successfully imported metadata";
        } catch (RiotException e) {
            log.error("Error importing metadata", e);
            return "Error: Invalid RDF content - " + e.getMessage();
        } catch (IOException e) {
            log.error("Error reading file", e);
            return "Error: Could not read file - " + e.getMessage();
        }
    }

    @ShellMethod(key = "remove-metadata", value = "Remove all metadata from the database")
    public String removeMetadata() {
        if (!quadImporterEnabled) {
            return "Quad importer is disabled";
        }

        try {
            quadImportService.removeMetadata();
            return "Successfully removed all metadata";
        } catch (Exception e) {
            log.error("Error removing metadata", e);
            return "Error: " + e.getMessage();
        }
    }

    @ShellMethod(key = "reset-database", value = "Reset the entire database (Warning: This will delete all data)")
    public String resetDatabase() {
        if (!quadImporterEnabled) {
            return "Quad importer is disabled";
        }

        try {
            quadImportService.resetDatabase();
            return "Successfully reset database";
        } catch (Exception e) {
            log.error("Error resetting database", e);
            return "Error: " + e.getMessage();
        }
    }
}
