package fr.vcity.sparqltosql.repository;

import fr.vcity.sparqltosql.dao.RDFVersionedNamedGraph;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class RDFVersionedNamedGraphComponent {
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public RDFVersionedNamedGraphComponent(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    public RDFVersionedNamedGraph save(String name, Integer length) {
        return namedParameterJdbcTemplate.queryForObject("""
                        INSERT INTO versioned_named_graph VALUES (DEFAULT, :name, (
                            SELECT LPAD('', :length, '0')::bit varying || B'1'
                        ))
                        ON CONFLICT (name) DO UPDATE SET name = EXCLUDED.name
                        RETURNING *
                        """,
                new MapSqlParameterSource()
                        .addValue("name", name)
                        .addValue("length", length),
                (rs, i) -> new RDFVersionedNamedGraph(
                        rs.getInt("id_named_graph"),
                        rs.getString("name"),
                        rs.getBytes("validity")
                ));
    }

    public void updateVersionedNamedGraphValidity() {
        namedParameterJdbcTemplate.getJdbcTemplate().execute("""
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
