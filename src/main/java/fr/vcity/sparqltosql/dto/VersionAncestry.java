package fr.vcity.sparqltosql.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class VersionAncestry {

    @Schema(name = "The sha of the version", example = "a1266ec24c2ba6c5852767178a875432")
    private String shaVersion;

    @Schema(name = "The parent sha", example = "bc64708ab69fe72581d3d0daa52bd640")
    private String shaVersionParent;

    @Schema(name = "The ancestry of the version", example = "{null,aa933ba74fe11812ec36d0b3c6dbaadc}")
    private List<String> ancestry;

    public VersionAncestry(String shaVersion, String shaVersionParent, List<String> ancestry) {
        this.shaVersion = shaVersion;
        this.shaVersionParent = shaVersionParent;
        this.ancestry = ancestry;
    }
}
