package fr.vcity.sparqltosql.dao;


import lombok.Data;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("versioned_quad")
public class RDFVersionedQuad {

    private Integer idSubject;

    private Integer idProperty;

    private Integer idObject;

    private Integer idNamedGraph;

    private byte[] validity;

    public RDFVersionedQuad(Integer idSubject, Integer idProperty, Integer idObject, Integer idNamedGraph, byte[] validity) {
        this.idSubject = idSubject;
        this.idProperty = idProperty;
        this.idObject = idObject;
        this.idNamedGraph = idNamedGraph;
        this.validity = validity;
    }

    public RDFVersionedQuad() {}

}
