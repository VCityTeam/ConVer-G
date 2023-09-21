package fr.vcity.sparqltosql.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Workspace {
    @Schema(
            name = "The consensus space data"
    )
    Space consensusSpace;

    @Schema(
            name = "The proposition space data"
    )
    Space propositionSpace;

    public Workspace(Space consensusSpace, Space propositionSpace) {
        this.consensusSpace = consensusSpace;
        this.propositionSpace = propositionSpace;
    }

    public Workspace() {
    }
}
