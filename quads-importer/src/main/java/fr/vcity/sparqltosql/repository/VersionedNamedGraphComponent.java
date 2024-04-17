package fr.vcity.sparqltosql.repository;

import fr.vcity.sparqltosql.dao.RDFVersionedNamedGraph;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class VersionedNamedGraphComponent {
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public VersionedNamedGraphComponent(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    public RDFVersionedNamedGraph save(Integer idVersionedNamedGraph, Integer index, Integer idNamedGraph) {
        return namedParameterJdbcTemplate.queryForObject("""
                        INSERT INTO versioned_named_graph VALUES (:idVersionedNamedGraph, :idNamedGraph, :index)
                        ON CONFLICT (id_versioned_named_graph) DO UPDATE SET id_named_graph = EXCLUDED.id_named_graph
                        RETURNING *
                        """,
                new MapSqlParameterSource()
                        .addValue("idVersionedNamedGraph", idVersionedNamedGraph)
                        .addValue("index", index)
                        .addValue("idNamedGraph", idNamedGraph),
                (rs, i) -> new RDFVersionedNamedGraph(
                        rs.getInt("id_versioned_named_graph"),
                        rs.getInt("index_version"),
                        rs.getInt("id_named_graph")
                ));
    }
}
