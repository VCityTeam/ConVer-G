package fr.vcity.sparqltosql.dao;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("commit")
public class RDFCommit {
    @Schema(name = "Commit ID", example = "1")
    private Integer idCommit;

    @Schema(name = "Message of the new commit", example = "") // TODO : Add example
    private String message;

    public RDFCommit() {
    }

    public RDFCommit(Integer idCommit, String message) {
        this.idCommit = idCommit;
        this.message = message;
    }
}
