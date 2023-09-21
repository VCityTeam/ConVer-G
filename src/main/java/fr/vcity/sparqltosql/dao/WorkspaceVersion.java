package fr.vcity.sparqltosql.dao;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Table("workspace_version")
public class WorkspaceVersion {
    @Id
    @Schema(name = "Index workspace version", example = "1")
    private Integer indexWorkspaceVersion;

    @Schema(name = "Message of the new Workspace", example = "Proposed a new representation of the city evolutions")
    private String message;

    @Schema(name = "Start of the validity's date", example = "2023-07-17 11:20:00+02")
    private LocalDateTime beginWorkspaceVersionDate;

    @Schema(name = "End of the validity's date", example = "2023-07-18 20:00:00+02")
    private LocalDateTime endWorkspaceVersionDate;

    public WorkspaceVersion() {
    }

    public WorkspaceVersion(
            Integer indexWorkspaceVersion,
            String message,
            LocalDateTime beginWorkspaceVersionDate,
            LocalDateTime endWorkspaceVersionDate
    ) {
        this.indexWorkspaceVersion = indexWorkspaceVersion;
        this.message = message;
        this.beginWorkspaceVersionDate = beginWorkspaceVersionDate;
        this.endWorkspaceVersionDate = endWorkspaceVersionDate;
    }
}
