package fr.vcity.sparqltosql.controllers;

import fr.vcity.sparqltosql.dto.RDFCompleteVersionedQuad;
import fr.vcity.sparqltosql.services.QuadQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/query")
public class QuadQueryController {
    QuadQueryService quadQueryService;

    public QuadQueryController(QuadQueryService quadQueryService) {
        this.quadQueryService = quadQueryService;
    }

    @Operation(summary = "Search all quads filtered by a given validity and returns the result")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The query filtered result",
                    content = {@Content(mediaType = "application/json")}),
            @ApiResponse(responseCode = "400", description = "Invalid validity",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Nothing found",
                    content = @Content)}
    )
    @PostMapping("/validity")
    List<RDFCompleteVersionedQuad> queryRequestedValidity(
            @RequestBody(description = "The validity string (in bit string format)", required = true)
            @org.springframework.web.bind.annotation.RequestBody String requestedValidity
    ) {
        return quadQueryService.queryRequestedValidity(requestedValidity);
    }

    @Operation(summary = "Find all quads filtered by a given version and returns the result")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The query filtered result",
                    content = {@Content(mediaType = "application/json")}),
            @ApiResponse(responseCode = "400", description = "Invalid version",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Nothing found",
                    content = @Content)}
    )
    @GetMapping("/version/{idVersion}")
    List<RDFCompleteVersionedQuad> queryRequestedVersion(
            @Parameter(description = "The version number")
            @PathVariable("idVersion") Integer requestedVersion
    ) {
        return quadQueryService.queryRequestedVersion(requestedVersion);
    }

    @Operation(summary = "Executes the SPARQL query and returns the result")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The query filtered result",
                    content = {@Content(mediaType = "application/json")}),
            @ApiResponse(responseCode = "400", description = "Invalid SPARQL request",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Nothing found",
                    content = @Content)}
    )
    @PostMapping("/sparql")
    void querySPARQL(
            @RequestBody(description = "The SPARQL query", required = true)
            @org.springframework.web.bind.annotation.RequestBody String queryString
    ) {
        quadQueryService.querySPARQL(queryString);
    }
}
