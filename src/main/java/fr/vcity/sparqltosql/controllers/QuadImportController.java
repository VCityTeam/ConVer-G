package fr.vcity.sparqltosql.controllers;

import fr.vcity.sparqltosql.services.QuadImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


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
            @ApiResponse(responseCode = "200", description = "The quads were added to a new version",
                    content = {
                            @Content(mediaType = "text/plain",
                                    schema = @Schema(implementation = Integer.class)
                            )}),
            @ApiResponse(responseCode = "500", description = "Invalid content")}
    )
    @PostMapping(value = {"/version"})
    ResponseEntity<Integer> importModel(
            @Parameter(description = "The file list containing all the triple/quads to import as valid in a new version", name = "files")
            @RequestParam("files") List<MultipartFile> files
    ) {
        return ResponseEntity.ok(quadImportService.importModel(files));
    }
}
