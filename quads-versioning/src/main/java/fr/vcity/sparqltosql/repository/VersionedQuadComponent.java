package fr.vcity.sparqltosql.repository;

import fr.vcity.sparqltosql.dto.CompleteVersionedQuad;
import fr.vcity.sparqltosql.services.QuadImportService;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.SQLException;
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

    public void saveResourceOrLiteral(List<QuadImportService.Node> nodes) {
        jdbcTemplate.batchUpdate("""
                INSERT INTO resource_or_literal (name, type)
                        VALUES (?, ?)
                        ON CONFLICT (sha512(resource_or_literal.name::bytea), (resource_or_literal.type)) DO UPDATE SET type = EXCLUDED.type
                        RETURNING *;
                """,
                new BatchPreparedStatementSetter() {
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        QuadImportService.Node node = nodes.get(i);
                        ps.setString(1, node.value());
                        ps.setString(2, node.type());
                    }

                    public int getBatchSize() {
                        return nodes.size();
                    }
                });
    }

    public void saveQuads(List<QuadImportService.QuadValueType> quadValueTypes) {
        jdbcTemplate.batchUpdate("""
                WITH a (
                     subject, subject_type,
                     predicate, predicate_type,
                     object, object_type,
                     named_graph,
                     version
                 ) AS (
                 VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        )
                        SELECT add_quad(
                            a.subject, a.subject_type,
                            a.predicate, a.predicate_type,
                            a.object, a.object_type,
                            a.named_graph,
                            a.version
                        ) FROM a;""",
                new BatchPreparedStatementSetter() {
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        ps.setString(1, quadValueTypes.get(i).tripleValueType().sValue());
                        ps.setString(2, quadValueTypes.get(i).tripleValueType().sType());
                        ps.setString(3, quadValueTypes.get(i).tripleValueType().pValue());
                        ps.setString(4, quadValueTypes.get(i).tripleValueType().pType());
                        ps.setString(5, quadValueTypes.get(i).tripleValueType().oValue());
                        ps.setString(6, quadValueTypes.get(i).tripleValueType().oType());
                        ps.setString(7, quadValueTypes.get(i).namedGraph());
                        ps.setInt(8, quadValueTypes.get(i).version());
                    }

                    public int getBatchSize() {
                        return quadValueTypes.size();
                    }
                });
    }
}
