package fr.vcity.sparqltosql.dao;


import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(name = "Resource or literal ID", example = "1")
    private Integer idResourceOrLiteral;

    @Schema(name = "Name of the resource or literal", example = "100")
    private String name;

    // NOT NULL if literal
    @Schema(name = "Type of the literal", example = "http://www.w3.org/2001/XMLSchema#string")
    private String type;
}
