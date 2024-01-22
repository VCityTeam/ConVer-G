package fr.vcity.sparqltosql.repository;

import fr.vcity.sparqltosql.dao.VersionedQuad;
import fr.vcity.sparqltosql.dto.CompleteVersionedQuad;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class VersionedQuadComponent {
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public VersionedQuadComponent(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    public List<CompleteVersionedQuad> findAll() {
        return namedParameterJdbcTemplate.query("""
                        SELECT rls.name, rlp.name, rlo.name, rlong.name, v.validity
                            FROM versioned_quad v LEFT JOIN resource_or_literal rls ON rls.id_resource_or_literal = v.id_subject
                            LEFT JOIN resource_or_literal rlp ON rlp.id_resource_or_literal = v.id_property
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
                            LEFT JOIN resource_or_literal rlp ON rlp.id_resource_or_literal = v.id_property
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
                            LEFT JOIN resource_or_literal rlp ON rlp.id_resource_or_literal = v.id_property
                            LEFT JOIN resource_or_literal rlo ON rlo.id_resource_or_literal = v.id_object
                            LEFT JOIN resource_or_literal rlong ON rlong.id_resource_or_literal = v.id_named_graph
                            WHERE get_bit(v.validity, :requestedVersion) = 1
                        """, new MapSqlParameterSource()
                        .addValue("requestedVersion", requestedVersion),
                getRdfCompleteVersionedQuadRowMapper()
        );
    }

    public VersionedQuad save(
            Integer idSubject,
            Integer idProperty,
            Integer idObject,
            Integer idNamedGraph,
            Integer length
    ) {
        return namedParameterJdbcTemplate.queryForObject("""
                        INSERT INTO versioned_quad (id_subject, id_property, id_object, id_named_graph, validity)
                        VALUES (:idSubject, :idProperty, :idObject, :idNamedGraph, (
                        SELECT LPAD('', :length, '0')::bit varying || B'1'
                        ) )
                        ON CONFLICT ON CONSTRAINT versioned_quad_pkey
                        DO UPDATE SET validity = versioned_quad.validity || B'1'
                        RETURNING *;
                        """,
                new MapSqlParameterSource()
                        .addValue("idSubject", idSubject)
                        .addValue("idProperty", idProperty)
                        .addValue("idObject", idObject)
                        .addValue("idNamedGraph", idNamedGraph)
                        .addValue("length", length),
                (rs, i) -> new VersionedQuad(
                        rs.getInt("id_subject"),
                        rs.getInt("id_property"),
                        rs.getInt("id_object"),
                        rs.getInt("id_named_graph"),
                        rs.getBytes("validity")
                )
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
}
