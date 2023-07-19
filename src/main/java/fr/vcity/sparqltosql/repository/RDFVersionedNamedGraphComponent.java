package fr.vcity.sparqltosql.repository;

import fr.vcity.sparqltosql.dao.RDFVersionedNamedGraph;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class RDFVersionedNamedGraphComponent {
    private final JdbcTemplate jdbcTemplate;

    public RDFVersionedNamedGraphComponent(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public RDFVersionedNamedGraph save(String name, Integer length) {
        String query = String.format("""
                INSERT INTO versioned_named_graph VALUES (DEFAULT, '%s', (
                    SELECT LPAD('', %s, '0')::bit varying || B'1'
                ))
                ON CONFLICT (name) DO UPDATE SET name = EXCLUDED.name
                RETURNING *
                """, name, length);
        return jdbcTemplate.queryForObject(query, (rs, i) -> new RDFVersionedNamedGraph(
                rs.getInt("id_named_graph"),
                rs.getString("name"),
                rs.getBytes("validity")
        ));
    }


    public void updateVersionedNamedGraphValidity() {
        jdbcTemplate.execute("""
            INSERT INTO versioned_named_graph (name, validity) (
                SELECT vng.name, bit_or(v.validity) FROM versioned_quad v
                    INNER JOIN versioned_named_graph vng on v.id_named_graph = vng.id_named_graph
                    GROUP BY vng.name
            )
            ON CONFLICT (name) DO UPDATE SET validity = EXCLUDED.validity
            RETURNING *
        """);
    }
}
