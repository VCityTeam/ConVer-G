package fr.vcity.converg.repository;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Slf4j
@Component
@DependsOnDatabaseInitialization
public class VersionedNamedGraphComponent {

    private final DataSource dataSource;

    public VersionedNamedGraphComponent(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void saveVersionedNamedGraph(List<String> namedGraphs, String originalFilename, Integer indexVersion) {
        try (Connection connection = dataSource.getConnection()) {

        for (List<String> partition : ListUtils.partition(namedGraphs, 100)) {
            String insertNamedGraphSQL = "SELECT version_named_graph(?, ?, ?)";
            try {
                PreparedStatement ps = connection.prepareStatement(insertNamedGraphSQL);
                for (String namedGraph : partition) {
                    ps.setString(1, namedGraph);
                    ps.setString(2, originalFilename);
                    ps.setInt(3, indexVersion);
                    ps.addBatch();
                }

                ps.executeBatch();
            } catch (SQLException e) {
                log.error("Error occurred in statement", e);
                throw new RuntimeException("Failed to save versioned named graph", e);
            }
        }
        } catch (SQLException e) {
            log.error("Error getting connection", e);
            throw new RuntimeException("Failed to get database connection", e);
        }
    }
}
