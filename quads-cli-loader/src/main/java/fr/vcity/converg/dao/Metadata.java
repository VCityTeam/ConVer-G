package fr.vcity.converg.dao;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("metadata")
public class Metadata {

    @Schema(name = "Subject ID", example = "1")
    private Integer idSubject;

    @Schema(name = "Predicate ID", example = "2")
    private Integer idPredicate;

    @Schema(name = "Object ID", example = "3")
    private Integer idObject;

    public Metadata(Integer idSubject, Integer idPredicate, Integer idObject) {
        this.idSubject = idSubject;
        this.idPredicate = idPredicate;
        this.idObject = idObject;
    }
}
