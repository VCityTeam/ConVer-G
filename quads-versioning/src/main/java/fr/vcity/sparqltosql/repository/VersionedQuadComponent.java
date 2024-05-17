package fr.vcity.sparqltosql.repository;

import fr.vcity.sparqltosql.dto.CompleteVersionedQuad;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class VersionedQuadComponent {
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final JdbcTemplate jdbcTemplate;

    public VersionedQuadComponent(NamedParameterJdbcTemplate namedParameterJdbcTemplate, JdbcTemplate jdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<CompleteVersionedQuad> findAll() {
        return namedParameterJdbcTemplate.query("""
                        SELECT rls.name, rlp.name, rlo.name, rlong.name, v.validity
                            FROM versioned_quad v LEFT JOIN resource_or_literal rls ON rls.id_resource_or_literal = v.id_subject
                            LEFT JOIN resource_or_literal rlp ON rlp.id_resource_or_literal = v.id_predicate
                            LEFT JOIN resource_or_literal rlo ON rlo.id_resource_or_literal = v.id_object
                            LEFT JOIN resource_or_literal rlong ON rlong.id_resource_or_literal = v.id_named_graph
                                """,
                getRdfCompleteVersionedQuadRowMapper()
        );
    }

    public List<CompleteVersionedQuad> findAllByValidity(String validity) {
        return namedParameterJdbcTemplate.query("""
                        SELECT rls.name, rlp.name, rlo.name, rlong.name, v.validity
                            FROM versioned_quad v LEFT JOIN resource_or_literal rls ON rls.id_resource_or_literal = v.id_subject
                            LEFT JOIN resource_or_literal rlp ON rlp.id_resource_or_literal = v.id_predicate
                            LEFT JOIN resource_or_literal rlo ON rlo.id_resource_or_literal = v.id_object
                            LEFT JOIN resource_or_literal rlong ON rlong.id_resource_or_literal = v.id_named_graph
                            WHERE v.validity = CAST(:validity as bit varying)
                        """, new MapSqlParameterSource()
                        .addValue("validity", validity),
                getRdfCompleteVersionedQuadRowMapper()
        );
    }

    public List<CompleteVersionedQuad> findAllByVersion(Integer requestedVersion) {
        return namedParameterJdbcTemplate.query("""
                        SELECT rls.name, rlp.name, rlo.name, rlong.name, v.validity
                            FROM versioned_quad v LEFT JOIN resource_or_literal rls ON rls.id_resource_or_literal = v.id_subject
                            LEFT JOIN resource_or_literal rlp ON rlp.id_resource_or_literal = v.id_predicate
                            LEFT JOIN resource_or_literal rlo ON rlo.id_resource_or_literal = v.id_object
                            LEFT JOIN resource_or_literal rlong ON rlong.id_resource_or_literal = v.id_named_graph
                            WHERE get_bit(v.validity, :requestedVersion) = 1
                        """, new MapSqlParameterSource()
                        .addValue("requestedVersion", requestedVersion),
                getRdfCompleteVersionedQuadRowMapper()
        );
    }


    private static RowMapper<CompleteVersionedQuad> getRdfCompleteVersionedQuadRowMapper() {
        return (rs, rowNum) -> new CompleteVersionedQuad(
                rs.getString(1),
                rs.getString(2),
                rs.getString(3),
                rs.getString(4),
                rs.getBytes(5)
        );
    }

    public void saveResourceOrLiteral(String rlQuery) {
        // should use a batch insert
        // https://www.baeldung.com/jdbc-batch-processing
        jdbcTemplate.execute("""
                WITH a (
                     node, node_type
                 ) AS (
                 VALUES""" + "\n" + rlQuery + """
                )
                SELECT add_quad_to_rl(
                    a.node, a.node_type
                ) FROM a;""");
    }

    public void saveQuads(String quadsQuery) {
        jdbcTemplate.execute("""
                WITH a (
                     subject, subject_type,
                     predicate, predicate_type,
                     object, object_type,
                     named_graph,
                     version
                 ) AS (
                 VALUES""" + "\n" + quadsQuery +
                """
                        )
                        SELECT add_quad(
                            a.subject, a.subject_type,
                            a.predicate, a.predicate_type,
                            a.object, a.object_type,
                            a.named_graph,
                            a.version
                        ) FROM a;""");
    }
}
