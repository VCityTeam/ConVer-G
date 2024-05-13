package fr.vcity.sparqltosql.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompleteVersionedQuad {

    @Schema(name = "The quad subject", example = "https://github.com/VCityTeam/UD-Graph/LYON_1ER_BATI_2015-1_bldg#BU_69381AB243_1")
    private String s;

    @Schema(name = "The quad predicate", example = "http://www.opengis.net/ont/geosparql#coordinateDimension")
    private String p;

    @Schema(name = "The quad object", example = "EPSG:32631")
    private String o;

    @Schema(name = "The quad named graph", example = "https://github.com/VCityTeam/VCity/City#Lyon")
    private String namedGraph;

    @Schema(name = "The quad validity", example = "1001")
    private byte[] validity;

    public CompleteVersionedQuad(
            String s,
            String p,
            String o,
            String namedGraph,
            byte[] validity
    ) {
        this.s = s;
        this.p = p;
        this.o = o;
        this.namedGraph = namedGraph;
        this.validity = validity;
    }

    public CompleteVersionedQuad() {
    }

}
