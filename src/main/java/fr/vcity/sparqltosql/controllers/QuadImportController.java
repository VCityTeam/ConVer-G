package fr.vcity.sparqltosql.controllers;

import fr.vcity.sparqltosql.services.QuadImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/import")
public class QuadImportController {
    QuadImportService quadImportService;

    public QuadImportController(QuadImportService quadImportService) {
        this.quadImportService = quadImportService;
    }

    @Operation(summary = "Adds all quads as a new version and considered as valid")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The quads were added to a new version"),
            @ApiResponse(responseCode = "500", description = "Invalid content")}
    )
    @PostMapping("/add")
    void importModelAdd(@Parameter(description = "The file list containing all the triple/quads to import as valid in a new version") @RequestParam("files") MultipartFile[] files) {
        quadImportService.importModelToAdd(files);
    }

    @Operation(summary = "Removes all quads as a new version and considered as invalid")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The quads were removed to a new version"),
            @ApiResponse(responseCode = "500", description = "Invalid content")}
    )
    @PostMapping("/remove")
    void importModelRemove(@Parameter(description = "The file list containing all the triple/quads to import as invalid in a new version") @RequestParam("files") MultipartFile[] files) {
        quadImportService.importModelToRemove(files);
    }

    @Operation(summary = "Removes all quads inside the 'to-remove' file then adds all quads inside the 'to-add' file as a new version")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The quads were treated as a new version"),
            @ApiResponse(responseCode = "500", description = "Invalid content")}
    )
    @PostMapping(value = "/remove-add")
    void submit(
            @Parameter(description = "The file list containing all the triple/quads to import as valid when filename contains 'add' and invalid when filename contains 'remove' in a new version")
            @RequestParam("files") MultipartFile[] files
    ) {
        quadImportService.importModelToRemoveAndAdd(files);
    }
}
