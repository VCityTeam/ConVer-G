package fr.vcity.sparqltosql.repository;

import fr.vcity.sparqltosql.connection.JdbcConnection;
import fr.vcity.sparqltosql.services.QuadImportService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
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

        for (List<QuadImportService.TripleValueType> partition : ListUtils.partition(tripleValueTypes, 100)) {
            String insertTriplesSQL = """
                    WITH a (
                     subject, subject_type,
                     predicate, predicate_type,
                     object, object_type
                 ) AS (
                """ + "VALUES (?, ?, ?, ?, ?, ?)" + """
                )
                SELECT add_triple(
                    a.subject, a.subject_type,
                    a.predicate, a.predicate_type,
                    a.object, a.object_type
                ) FROM a;""";
            try {
                PreparedStatement ps = connection.prepareStatement(insertTriplesSQL);
                for (QuadImportService.TripleValueType tripleValueType : partition) {
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
            }
        }
    }
}
