package fr.vcity.sparqltosql.dao;


import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Data
@Table(name = "resource_or_literal")
public class RDFResourceOrLiteral {

    public RDFResourceOrLiteral() {}

    public RDFResourceOrLiteral(String name, String type) {
        this.name = name;
        this.type = type;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(columnDefinition = "TEXT")
    private String name;

    // NOT NULL if literal
    private String type;
}
