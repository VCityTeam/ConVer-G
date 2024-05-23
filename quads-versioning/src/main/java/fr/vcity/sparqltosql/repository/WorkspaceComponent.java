package fr.vcity.sparqltosql.repository;

import fr.vcity.sparqltosql.services.QuadImportService;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Component
public class WorkspaceComponent {
    private final JdbcTemplate jdbcTemplate;

    public WorkspaceComponent(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void saveTriples(List<QuadImportService.TripleValueType> tripleValueTypes) {
        jdbcTemplate.batchUpdate("""
                WITH a (
                     subject, subject_type,
                     predicate, predicate_type,
                     object, object_type
                 ) AS (
                 VALUES (?, ?, ?, ?, ?, ?)
                )
                SELECT add_triple(
                    a.subject, a.subject_type,
                    a.predicate, a.predicate_type,
                    a.object, a.object_type
                ) FROM a;""",
                new BatchPreparedStatementSetter() {
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        ps.setString(1, tripleValueTypes.get(i).sValue());
                        ps.setString(2, tripleValueTypes.get(i).sType());
                        ps.setString(3, tripleValueTypes.get(i).pValue());
                        ps.setString(4, tripleValueTypes.get(i).pType());
                        ps.setString(5, tripleValueTypes.get(i).oValue());
                        ps.setString(6, tripleValueTypes.get(i).oType());
                    }

                    public int getBatchSize() {
                        return tripleValueTypes.size();
                    }
                });
    }
}
