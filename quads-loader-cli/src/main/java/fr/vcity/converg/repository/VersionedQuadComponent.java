package fr.vcity.converg.repository;

import fr.vcity.converg.services.QuadImportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Component
@DependsOnDatabaseInitialization
@Slf4j
public class VersionedQuadComponent {

    private final DataSource dataSource;

    public VersionedQuadComponent(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void flattenVersionedQuads() {
        try (Connection connection = dataSource.getConnection()) {

        String insertFlatQuadSQL = """
                INSERT INTO versioned_quad_flat (id_subject, id_predicate, id_object, id_versioned_named_graph)
                SELECT v.id_subject, v.id_predicate, v.id_object, vng.id_versioned_named_graph
                FROM versioned_quad v
                JOIN versioned_named_graph vng ON get_bit(v.validity, vng.index_version - 1) = 1
                    AND vng.id_named_graph = v.id_named_graph;""";
        try {
            PreparedStatement ps = connection.prepareStatement(insertFlatQuadSQL);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error occurred in statement", e);
            throw new RuntimeException("Failed to flatten versioned quads", e);
        }
        } catch (SQLException e) {
            log.error("Error getting connection", e);
            throw new RuntimeException("Failed to get database connection", e);
        }
    }

    public void saveQuads(List<QuadImportService.QuadValueType> quadValueTypes) {
        try (Connection connection = dataSource.getConnection()) {

        String insertQuadValueSQL = """
                INSERT INTO flat_model_quad (subject, subject_type, predicate, predicate_type, object, object_type, named_graph, version)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?);""";

        try {
            PreparedStatement ps = connection.prepareStatement(insertQuadValueSQL);

            for (QuadImportService.QuadValueType quadValueType : quadValueTypes) {
                ps.setString(1, removeAllSpecialCharacters(quadValueType.tripleValueType().sValue()));
                ps.setString(2, removeAllSpecialCharacters(quadValueType.tripleValueType().sType()));
                ps.setString(3, removeAllSpecialCharacters(quadValueType.tripleValueType().pValue()));
                ps.setString(4, removeAllSpecialCharacters(quadValueType.tripleValueType().pType()));
                ps.setString(5, removeAllSpecialCharacters(quadValueType.tripleValueType().oValue()));
                ps.setString(6, removeAllSpecialCharacters(quadValueType.tripleValueType().oType()));
                ps.setString(7, removeAllSpecialCharacters(quadValueType.namedGraph()));
                ps.setInt(8, quadValueType.version());
                ps.addBatch();
            }

            ps.executeBatch();
        } catch (SQLException e) {
            log.error("Error occurred in statement", e);
            throw new RuntimeException("Failed to save quads", e);
        }
        } catch (SQLException e) {
            log.error("Error getting connection", e);
            throw new RuntimeException("Failed to get database connection", e);
        }
    }
    
    public String removeAllSpecialCharacters(String input) {
        if (input == null) {
            return null;
        }
        return input
            .replace("\n", " ")
            .replace("\r", " ")
            .replace("\t", " ")
            .replace("\\", "\\\\");
    }
}
