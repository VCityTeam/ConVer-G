package fr.vcity.converg.repository;

import fr.vcity.converg.connection.JdbcConnection;
import fr.vcity.converg.services.QuadImportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Component
@Slf4j
public class VersionedQuadComponent {

    public VersionedQuadComponent() {
    }

    public void flattenVersionedQuads() {
        JdbcConnection jdbcConnection = JdbcConnection.getInstance();
        Connection connection = jdbcConnection.getConnection();

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
        }
    }

    public void saveQuads(List<QuadImportService.QuadValueType> quadValueTypes) {
        JdbcConnection jdbcConnection = JdbcConnection.getInstance();
        Connection connection = jdbcConnection.getConnection();

        String insertQuadValueSQL = """
                INSERT INTO flat_model_quad (subject, subject_type, predicate, predicate_type, object, object_type, named_graph, version)"""
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?);";

        try {
            PreparedStatement ps = connection.prepareStatement(insertQuadValueSQL);

            for (QuadImportService.QuadValueType quadValueType : quadValueTypes) {
                ps.setString(1, quadValueType.tripleValueType().sValue());
                ps.setString(2, quadValueType.tripleValueType().sType());
                ps.setString(3, quadValueType.tripleValueType().pValue());
                ps.setString(4, quadValueType.tripleValueType().pType());
                ps.setString(5, quadValueType.tripleValueType().oValue());
                ps.setString(6, quadValueType.tripleValueType().oType());
                ps.setString(7, quadValueType.namedGraph());
                ps.setInt(8, quadValueType.version());
                ps.addBatch();
            }

            ps.executeBatch();
        } catch (SQLException e) {
            log.error("Error occurred in statement", e);
        }
    }
}
