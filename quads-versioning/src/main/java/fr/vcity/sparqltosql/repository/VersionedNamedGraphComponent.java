package fr.vcity.sparqltosql.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class VersionedNamedGraphComponent {
    private final JdbcTemplate jdbcTemplate;

    public VersionedNamedGraphComponent(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void saveVersionedNamedGraph(String sql) {
        jdbcTemplate.execute("""
                  WITH a (
                        named_graph,
                        filename,
                        version
                    ) AS (
                    VALUES""" + "\n" + sql + """
                    )
                    SELECT version_named_graph(
                        a.named_graph,
                        a.filename,
                        a.version
                    ) FROM a;"""
        );
    }
}
