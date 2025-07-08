package fr.vcity.converg.dao;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("versioned_quad_flat")
public class VersionedQuadFlat {

    @Schema(name = "Subject ID", example = "1")
    private Integer idSubject;

    @Schema(name = "Predicate ID", example = "2")
    private Integer idPredicate;

    @Schema(name = "Object ID", example = "3")
    private Integer idObject;

    @Schema(name = "Named Graph ID", example = "4")
    private Integer idNamedGraph;

    @Schema(name = "Index version", example = "1")
    private Integer indexVersion;

    public VersionedQuadFlat(Integer idSubject, Integer idPredicate, Integer idObject, Integer idNamedGraph, Integer indexVersion) {
        this.idSubject = idSubject;
        this.idPredicate = idPredicate;
        this.idObject = idObject;
        this.idNamedGraph = idNamedGraph;
        this.indexVersion = indexVersion;
    }
}
