package fr.vcity.sparqltosql.repository;

import fr.vcity.sparqltosql.dao.RDFVersionedNamedGraph;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class VersionedNamedGraphComponent {
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final JdbcTemplate jdbcTemplate;

    public VersionedNamedGraphComponent(
            NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            JdbcTemplate jdbcTemplate
    ) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
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
