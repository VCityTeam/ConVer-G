package fr.vcity.sparqltosql.dao;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Table("version")
public class Version {
    @Schema(name = "Index version", example = "1")
    private Integer indexVersion;

    @Schema(name = "Sha of the version", example = "8d8e5de665072b1c60c41100338aa2e6")
    private String shaVersion;

    @Schema(name = "Sha of the parent", example = "aebc5a484a84ca84c8a428ca428c4da2")
    private String shaVersionParent;

    @Schema(name = "Message of the new version", example = "Inserted trees and removed building")
    private String message;

    @Schema(name = "Start of the validity's date", example = "2023-07-17 11:20:00+02")
    private LocalDateTime beginVersionDate;

    @Schema(name = "End of the validity's date", example = "2023-07-18 20:00:00+02")
    private LocalDateTime endVersionDate;

    public Version() {
    }

    public Version(
            Integer indexVersion,
            String shaVersion,
            String shaVersionParent,
            String message,
            LocalDateTime beginVersionDate,
            LocalDateTime endVersionDate
    ) {
        this.indexVersion = indexVersion;
        this.shaVersion = shaVersion;
        this.shaVersionParent = shaVersionParent;
        this.message = message;
        this.beginVersionDate = beginVersionDate;
        this.endVersionDate = endVersionDate;
    }
}
