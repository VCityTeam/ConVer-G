package fr.vcity.converg.repository;

import fr.vcity.converg.connection.JdbcConnection;
import fr.vcity.converg.services.QuadImportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Slf4j
@Component
public class MetadataComponent {

    public void saveTriples(List<QuadImportService.TripleValueType> tripleValueTypes) {
        JdbcConnection jdbcConnection = JdbcConnection.getInstance();
        Connection connection = jdbcConnection.getConnection();

        String insertTripleValueSQL = """
                INSERT INTO flat_model_triple (subject, subject_type, predicate, predicate_type, object, object_type)"""
                + "VALUES (?, ?, ?, ?, ?, ?);";

        try {
            PreparedStatement ps = connection.prepareStatement(insertTripleValueSQL);

            for (QuadImportService.TripleValueType tripleValueType : tripleValueTypes) {
                ps.setString(1, tripleValueType.sValue());
                ps.setString(2, tripleValueType.sType());
                ps.setString(3, tripleValueType.pValue());
                ps.setString(4, tripleValueType.pType());
                ps.setString(5, tripleValueType.oValue());
                ps.setString(6, tripleValueType.oType());
                ps.addBatch();
            }

            ps.executeBatch();
        } catch (SQLException e) {
            log.error("Error occurred in statement", e);
            throw new RuntimeException("Failed to save triples", e);
        }
    }
}
