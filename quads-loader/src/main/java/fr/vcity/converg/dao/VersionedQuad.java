package fr.vcity.converg.dao;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("versioned_quad")
public class VersionedQuad {

    @Schema(name = "Subject ID", example = "1")
    private Integer idSubject;

    @Schema(name = "Predicate ID", example = "2")
    private Integer idPredicate;

    @Schema(name = "Object ID", example = "3")
    private Integer idObject;

    @Schema(name = "Named Graph ID", example = "4")
    private Integer idNamedGraph;

    @Schema(name = "Validity", example = "B'10001'")
    private byte[] validity;

    public VersionedQuad(Integer idSubject, Integer idPredicate, Integer idObject, Integer idNamedGraph, byte[] validity) {
        this.idSubject = idSubject;
        this.idPredicate = idPredicate;
        this.idObject = idObject;
        this.idNamedGraph = idNamedGraph;
        this.validity = validity;
    }
}
