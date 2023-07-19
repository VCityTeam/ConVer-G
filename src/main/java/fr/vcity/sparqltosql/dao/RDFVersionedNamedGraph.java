package fr.vcity.sparqltosql.dao;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("versioned_named_graph")
public class RDFVersionedNamedGraph {

    @Schema(name = "Named Graph ID", example = "1")
    private Integer idNamedGraph;

    @Schema(name = "Name of the graph", example = "https://github.com/VCityTeam/VCity/City#Lyon")
    private String name;

    @Schema(name = "Validity", example = "B'10001'")
    private byte[] validity;

    public RDFVersionedNamedGraph(Integer idNamedGraph, String name, byte[] validity) {
        this.idNamedGraph = idNamedGraph;
        this.name = name;
        this.validity = validity;
    }
}
