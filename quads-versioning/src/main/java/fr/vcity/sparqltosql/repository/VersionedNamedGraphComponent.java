package fr.vcity.sparqltosql.repository;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Component
public class VersionedNamedGraphComponent {
    private final JdbcTemplate jdbcTemplate;

    public VersionedNamedGraphComponent(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void saveVersionedNamedGraph(List<String> namedGraphs, String originalFilename, Integer indexVersion) {
        jdbcTemplate.batchUpdate("""
                  WITH a (
                        named_graph,
                        filename,
                        version
                    ) AS (
                    VALUES (?,?,?)
                    )
                    SELECT version_named_graph(
                        a.named_graph,
                        a.filename,
                        a.version
                    ) FROM a;""",
                new BatchPreparedStatementSetter() {
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        ps.setString(1, namedGraphs.get(i));
                        ps.setString(2, originalFilename);
                        ps.setInt(3, indexVersion);
                    }

                    public int getBatchSize() {
                        return namedGraphs.size();
                    }
                });
    }
}
