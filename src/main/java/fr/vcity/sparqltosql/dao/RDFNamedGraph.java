package fr.vcity.sparqltosql.dao;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("named_graph")
public class RDFNamedGraph {

    @Schema(name = "Named Graph ID", example = "1")
    private Integer idNamedGraph;

    @Schema(name = "Name of the graph", example = "https://github.com/VCityTeam/VCity/City#Lyon")
    private String name;

    public RDFNamedGraph() {
    }

    public RDFNamedGraph(String name) {
        this.name = name;
    }
}
