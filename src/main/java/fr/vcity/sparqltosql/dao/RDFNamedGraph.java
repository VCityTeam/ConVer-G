package fr.vcity.sparqltosql.dao;

import lombok.Data;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("named_graph")
public class RDFNamedGraph {

    private Integer idNamedGraph;

    private String name;

    public RDFNamedGraph() {
    }

    public RDFNamedGraph(String name) {
        this.name = name;
    }
}
