package fr.cnrs.liris.jpugetgil.sparqltosql.dao;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "versioned_named_graph")
public class VersionedNamedGraph {
    @Id
    private Integer idVersionedNamedGraph;

    private Integer idNamedGraph;

    private Integer index;

    public VersionedNamedGraph(Integer idNamedGraph, Integer index, Integer idVersionedNamedGraph) {
        this.idNamedGraph = idNamedGraph;
        this.index = index;
        this.idVersionedNamedGraph = idVersionedNamedGraph;
    }

    public VersionedNamedGraph() {

    }
}
