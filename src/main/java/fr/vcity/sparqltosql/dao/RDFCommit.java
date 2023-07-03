package fr.vcity.sparqltosql.dao;

import lombok.Data;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("commit")
public class RDFCommit {
    private Integer idCommit;

    private String message;

    public RDFCommit() {
    }

    public RDFCommit(Integer idCommit, String message) {
        this.idCommit = idCommit;
        this.message = message;
    }
}
