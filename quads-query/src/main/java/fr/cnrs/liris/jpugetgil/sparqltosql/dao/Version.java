package fr.cnrs.liris.jpugetgil.sparqltosql.dao;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "version")
public class Version {
    @Id
    private Integer indexVersion;

    private String message;

    private LocalDateTime transactionTimeStart;

    private LocalDateTime transactionTimeEnd;

    public Version() {
    }

    public Version(
            Integer indexVersion,
            String message,
            LocalDateTime transactionTimeStart,
            LocalDateTime transactionTimeEnd
    ) {
        this.indexVersion = indexVersion;
        this.message = message;
        this.transactionTimeStart = transactionTimeStart;
        this.transactionTimeEnd = transactionTimeEnd;
    }
}
