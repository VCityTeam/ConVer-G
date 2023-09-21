package fr.vcity.sparqltosql.dao;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("versioned_workspace")
public class VersionedWorkspace {

    @Schema(name = "Subject ID", example = "1")
    private Integer idSubject;

    @Schema(name = "Property ID", example = "2")
    private Integer idProperty;

    @Schema(name = "Object ID", example = "3")
    private Integer idObject;

    @Schema(name = "Validity", example = "B'10001'")
    private byte[] validity;

    public VersionedWorkspace(Integer idSubject, Integer idProperty, Integer idObject, byte[] validity) {
        this.idSubject = idSubject;
        this.idProperty = idProperty;
        this.idObject = idObject;
        this.validity = validity;
    }

    public VersionedWorkspace() {}

}
