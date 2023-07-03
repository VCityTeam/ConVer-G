package fr.vcity.sparqltosql.dao;


import lombok.Data;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("resource_or_literal")
public class RDFResourceOrLiteral {

    public RDFResourceOrLiteral() {}

    public RDFResourceOrLiteral(Integer idResourceOrLiteral, String name, String type) {
        this.idResourceOrLiteral = idResourceOrLiteral;
        this.name = name;
        this.type = type;
    }

    public RDFResourceOrLiteral(String name, String type) {
        this.name = name;
        this.type = type;
    }

    private Integer idResourceOrLiteral;

    private String name;

    // NOT NULL if literal
    private String type;
}
