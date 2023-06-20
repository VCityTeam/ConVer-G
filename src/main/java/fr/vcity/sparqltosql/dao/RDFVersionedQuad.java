package fr.vcity.sparqltosql.dao;


import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.util.BitSet;

@Entity
@Data
@IdClass(CompositeRDFVersionedQuad.class)
@Table(name = "versioned_quad")
public class RDFVersionedQuad {

    @Id
    private Integer idSubject;

    @Id
    private Integer idProperty;

    @Id
    private Integer idObject;

    private Integer idNamedGraph;

    @Column(columnDefinition = "TEXT")
    private String validity;

    public RDFVersionedQuad(Integer idSubject, Integer idProperty, Integer idObject, Integer idNamedGraph, String validity) {
        this.idSubject = idSubject;
        this.idProperty = idProperty;
        this.idObject = idObject;
        this.idNamedGraph = idNamedGraph;
        this.validity = validity;
    }

    public RDFVersionedQuad() {}

}
