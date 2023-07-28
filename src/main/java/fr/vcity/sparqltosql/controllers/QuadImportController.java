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
                                    schema = @Schema(implementation = String.class)
                            )}),
            @ApiResponse(responseCode = "500", description = "Invalid content")}
    )
    @PostMapping(value = {"/add", "/add/{shaParent}"})
    ResponseEntity<String> importModelAdd(
            @Parameter(description = "The sha of the parent version", name = "shaParent", example = "8d8e5de665072b1c60c41100338aa2e6")
            @PathVariable(value = "shaParent", required = false) String shaParent,
            @Parameter(description = "The file list containing all the triple/quads to import as valid in a new version", name = "files")
            @RequestParam("files") List<MultipartFile> files
    ) {
        return ResponseEntity.ok(quadImportService.importModelToAdd(shaParent, files));
    }

    @Operation(
            summary = "Removes quads and creates a new version",
            description = "Removes all quads as a new version and considered as invalid"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The quads were removed to a new version",
                    content = {
                            @Content(mediaType = "text/plain",
                                    schema = @Schema(implementation = String.class)
                            )}),
            @ApiResponse(responseCode = "500", description = "Invalid content")}
    )
    @PostMapping(value = {"/remove", "/remove/{shaParent}"})
    ResponseEntity<String> importModelRemove(
            @Parameter(description = "The sha of the parent version", name = "shaParent", example = "8d8e5de665072b1c60c41100338aa2e6")
            @PathVariable(value = "shaParent", required = false) String shaParent,
            @Parameter(description = "The file list containing all the triple/quads to import as invalid in a new version", name = "files")
            @RequestParam("files") List<MultipartFile> files
    ) {
        return ResponseEntity.ok(quadImportService.importModelToRemove(shaParent, files));
    }

    @Operation(
            summary = "Adds, removes quads and creates a new version",
            description = "Removes all quads inside the 'to-remove' file then adds all quads inside the 'to-add' file as a new version"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The quads were treated as a new version",
                    content = {
                            @Content(mediaType = "text/plain",
                                    schema = @Schema(implementation = String.class)
                            )}),
            @ApiResponse(responseCode = "500", description = "Invalid content")}
    )
    @PostMapping(value = {"/remove-add", "/remove-add/{shaParent}"})
    ResponseEntity<String> submit(
            @Parameter(description = "The sha of the parent version", name = "shaParent", example = "8d8e5de665072b1c60c41100338aa2e6")
            @PathVariable(value = "shaParent", required = false) String shaParent,
            @Parameter(description = "The file list containing all the triple/quads to import as valid when filename contains 'add' and invalid when filename contains 'remove' in a new version", name = "files")
            @RequestParam("files") List<MultipartFile> files
    ) {
        return ResponseEntity.ok(quadImportService.importModelToRemoveAndAdd(shaParent, files));
    }
}
