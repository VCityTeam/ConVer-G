package fr.cnrs.liris.jpugetgil.sparqltosql.dao;


import jakarta.persistence.*;

@Entity
@IdClass(VersionedQuadComposite.class)
@Table(name = "versioned_quad")
public class VersionedQuad {

    @Id
    @Column(name = "id_subject")
    private Integer idSubject;

    @Id
    @Column(name = "id_property")
    private Integer idProperty;

    @Id
    @Column(name = "id_object")
    private Integer idObject;

    @Id
    @Column(name = "id_named_graph")
    private Integer idNamedGraph;

    private byte[] validity;

    @ManyToOne
    @JoinColumn(name = "id_subject", referencedColumnName = "id_resource_or_literal", nullable = false)
    private ResourceOrLiteral subject;

    @ManyToOne
    @JoinColumn(name = "id_property", referencedColumnName = "id_resource_or_literal", nullable = false)
    private ResourceOrLiteral property;

    @ManyToOne
    @JoinColumn(name = "id_object", referencedColumnName = "id_resource_or_literal", nullable = false)
    private ResourceOrLiteral object;

    @ManyToOne
    @JoinColumn(name = "id_named_graph", referencedColumnName = "id_resource_or_literal", nullable = false)
    private ResourceOrLiteral namedGraph;

    public VersionedQuad(Integer idSubject, Integer idProperty, Integer idObject, Integer idNamedGraph, byte[] validity) {
        this.idSubject = idSubject;
        this.idProperty = idProperty;
        this.idObject = idObject;
        this.idNamedGraph = idNamedGraph;
        this.validity = validity;
    }

    public VersionedQuad() {

    }

    public Integer getIdSubject() {
        return idSubject;
    }

    public void setIdSubject(Integer idSubject) {
        this.idSubject = idSubject;
    }

    public Integer getIdProperty() {
        return idProperty;
    }

    public void setIdProperty(Integer idProperty) {
        this.idProperty = idProperty;
    }

    public Integer getIdObject() {
        return idObject;
    }

    public void setIdObject(Integer idObject) {
        this.idObject = idObject;
    }

    public Integer getIdNamedGraph() {
        return idNamedGraph;
    }

    public void setIdNamedGraph(Integer idNamedGraph) {
        this.idNamedGraph = idNamedGraph;
    }

    public byte[] getValidity() {
        return validity;
    }

    public void setValidity(byte[] validity) {
        this.validity = validity;
    }
}
