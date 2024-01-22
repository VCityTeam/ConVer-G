package fr.cnrs.liris.jpugetgil.sparqltosql.dao;

import java.io.Serializable;

public class WorkspaceComposite implements Serializable  {
    private Integer idSubject;

    private Integer idProperty;

    private Integer idObject;

    public WorkspaceComposite(Integer idSubject, Integer idProperty, Integer idObject) {
        this.idSubject = idSubject;
        this.idProperty = idProperty;
        this.idObject = idObject;
    }
}
