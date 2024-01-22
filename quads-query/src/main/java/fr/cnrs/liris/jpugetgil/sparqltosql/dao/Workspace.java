package fr.cnrs.liris.jpugetgil.sparqltosql.dao;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@IdClass(WorkspaceComposite.class)
@Table(name = "workspace")
public class Workspace {

    @Id
    private Integer idSubject;

    @Id
    private Integer idProperty;

    @Id
    private Integer idObject;

    public Workspace(Integer idSubject, Integer idProperty, Integer idObject) {
        this.idSubject = idSubject;
        this.idProperty = idProperty;
        this.idObject = idObject;
    }

    public Workspace() {
    }

}
