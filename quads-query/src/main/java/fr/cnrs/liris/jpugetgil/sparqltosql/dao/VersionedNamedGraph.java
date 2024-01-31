package fr.cnrs.liris.jpugetgil.sparqltosql.dao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "versioned_named_graph")
public class VersionedNamedGraph {
    @Id
    @Column(name = "id_versioned_named_graph")
    private Integer idVersionedNamedGraph;

    @Column(name = "id_named_graph")
    private Integer idNamedGraph;

    @Column(name = "index_version")
    private Integer index;

    public VersionedNamedGraph(Integer idNamedGraph, Integer index, Integer idVersionedNamedGraph) {
        this.idNamedGraph = idNamedGraph;
        this.index = index;
        this.idVersionedNamedGraph = idVersionedNamedGraph;
    }

    public VersionedNamedGraph() {
    }

    public Integer getIdVersionedNamedGraph() {
        return idVersionedNamedGraph;
    }

    public Integer getIdNamedGraph() {
        return idNamedGraph;
    }

    public Integer getIndex() {
        return index;
    }
}
