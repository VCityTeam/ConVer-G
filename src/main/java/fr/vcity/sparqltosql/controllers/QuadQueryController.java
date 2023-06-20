package fr.vcity.sparqltosql.controllers;

import fr.vcity.sparqltosql.dto.RDFCompleteVersionedQuad;
import fr.vcity.sparqltosql.services.QuadQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/quads")
public class QuadQueryController {
    QuadQueryService quadQueryService;

    public QuadQueryController(QuadQueryService quadQueryService) {
        this.quadQueryService = quadQueryService;
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
    @PostMapping("/query")
    List<RDFCompleteVersionedQuad> query(@RequestBody String requestedVersion) {
        return quadQueryService.query(requestedVersion);
    }

    @PostMapping("/query2")
    void query2(@RequestBody String queryString) {
        quadQueryService.query2(queryString);
    }

    @PostMapping("/import/{lang}")
    void importModel(@RequestBody String model, @PathVariable String lang) {
        quadQueryService.importModel(model, lang);
    }
}
