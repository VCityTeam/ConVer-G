package fr.vcity.sparqltosql.dao;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "named_graph")
public class RDFNamedGraph {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;

    public RDFNamedGraph() {
    }

    public RDFNamedGraph(String name) {
        this.name = name;
    }
}
