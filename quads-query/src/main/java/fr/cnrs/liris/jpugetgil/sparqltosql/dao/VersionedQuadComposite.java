package fr.cnrs.liris.jpugetgil.sparqltosql.dao;

import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class VersionedQuadComposite implements Serializable {
    private Integer idSubject;

    private Integer idProperty;

    private Integer idObject;

    private Integer idNamedGraph;

    public VersionedQuadComposite(Integer idSubject, Integer idProperty, Integer idObject, Integer idNamedGraph) {
        this.idSubject = idSubject;
        this.idProperty = idProperty;
        this.idObject = idObject;
        this.idNamedGraph = idNamedGraph;
    }

    public VersionedQuadComposite() {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VersionedQuadComposite that)) return false;
        return Objects.equals(getIdSubject(), that.getIdSubject()) && Objects.equals(getIdProperty(), that.getIdProperty()) && Objects.equals(getIdObject(), that.getIdObject()) && Objects.equals(getIdNamedGraph(), that.getIdNamedGraph());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getIdSubject(), getIdProperty(), getIdObject(), getIdNamedGraph());
    }
}
