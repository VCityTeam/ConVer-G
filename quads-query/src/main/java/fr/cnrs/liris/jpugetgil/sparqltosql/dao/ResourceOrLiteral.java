package fr.cnrs.liris.jpugetgil.sparqltosql.dao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "resource_or_literal")
public class ResourceOrLiteral {
    @Id
    @Column(name = "id_resource_or_literal")
    private Integer idResourceOrLiteral;

    @Column(name = "name")
    private String name;

    @Column(name = "type")
    private String type;

    public ResourceOrLiteral(Integer idResourceOrLiteral, String name, String type) {
        this.idResourceOrLiteral = idResourceOrLiteral;
        this.name = name;
        this.type = type;
    }

    public ResourceOrLiteral() {
    }
}
