package fr.vcity.converg.controllers;

import fr.vcity.converg.services.QuadImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.jena.riot.RiotException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@RestController
@Tag(name = "Import API")
@RequestMapping("/import")
public class QuadImportController {
    QuadImportService quadImportService;

    public QuadImportController(QuadImportService quadImportService) {
        this.quadImportService = quadImportService;
    }

    @Operation(
            summary = "Adds quads and creates a new version",
            description = "Adds all quads as a new version and considered as valid"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The quads were added to a new version"),
            @ApiResponse(responseCode = "400", description = "Invalid content")}
    )
    @PostMapping(value = {"/version"})
    ResponseEntity<Integer> importModel(
            @Parameter(description = "The file containing all the triple/quads to import as valid in a new version", name = "file")
            @RequestParam("file") MultipartFile file
    ) {
        try {
            Integer indexVersion = quadImportService.importModel(file);
            return ResponseEntity.ok(indexVersion);
        } catch (RiotException e) {
            return ResponseEntity
                    .badRequest()
                    .body(null);
        }
    }

    @Operation(
            summary = "Adds triple and creates a new metadata",
            description = "Adds all triple as a new metadata and considered as valid"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The triple were added to a new metadata"),
            @ApiResponse(responseCode = "400", description = "Invalid content")}
    )
    @PostMapping(value = {"/metadata"})
    ResponseEntity<Void> importMetadata(
            @Parameter(description = "The file containing all the triple to import as valid in a new metadata", name = "file")
            @RequestParam("file") MultipartFile file
    ) {
        try {
            quadImportService.importMetadata(file);
            return ResponseEntity.ok().build();
        } catch (RiotException e) {
            return ResponseEntity
                    .badRequest()
                    .body(null);
        }
    }

    @Operation(
            summary = "Remove the current metadata",
            description = "Make the current metadata as null"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The metadata have been cleaned")
    })
    @DeleteMapping(value = {"/metadata"})
    void removeMetadata() {
        quadImportService.removeMetadata();
    }
}
