package fr.vcity.sparqltosql.repository;

import fr.vcity.sparqltosql.dto.RDFCompleteVersionedQuad;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RDFVersionedQuadComponent {
    private final JdbcTemplate jdbcTemplate;

    public RDFVersionedQuadComponent(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<RDFCompleteVersionedQuad> findAll() {
        return jdbcTemplate.query("""
                        SELECT rls.name, rlp.name, rlo.name, ng.name, v.validity
                            FROM versioned_quad v LEFT JOIN resource_or_literal rls ON rls.id_resource_or_literal = v.id_subject
                            LEFT JOIN resource_or_literal rlp ON rlp.id_resource_or_literal = v.id_property
                            LEFT JOIN resource_or_literal rlo ON rlo.id_resource_or_literal = v.id_object
                            LEFT JOIN named_graph ng ON ng.id_named_graph = v.id_named_graph
                                """,
                getRdfCompleteVersionedQuadRowMapper()
        );
    }

    public List<RDFCompleteVersionedQuad> findAllByValidity(String validity) {
        // FIXME : SQL injection !
        String query = String.format("""
                SELECT rls.name, rlp.name, rlo.name, ng.name, v.validity
                    FROM versioned_quad v LEFT JOIN resource_or_literal rls ON rls.id_resource_or_literal = v.id_subject
                    LEFT JOIN resource_or_literal rlp ON rlp.id_resource_or_literal = v.id_property
                    LEFT JOIN resource_or_literal rlo ON rlo.id_resource_or_literal = v.id_object
                    LEFT JOIN named_graph ng ON ng.id_named_graph = v.id_named_graph
                    WHERE v.validity = B'%s'
                """, validity);
        return jdbcTemplate.query(query,
                getRdfCompleteVersionedQuadRowMapper()
        );
    }

    public List<RDFCompleteVersionedQuad> findAllByVersion(Integer requestedVersion) {
        // FIXME : SQL injection !
        String query = String.format("""
                SELECT rls.name, rlp.name, rlo.name, ng.name, v.validity
                    FROM versioned_quad v LEFT JOIN resource_or_literal rls ON rls.id_resource_or_literal = v.id_subject
                    LEFT JOIN resource_or_literal rlp ON rlp.id_resource_or_literal = v.id_property
                    LEFT JOIN resource_or_literal rlo ON rlo.id_resource_or_literal = v.id_object
                    LEFT JOIN named_graph ng ON ng.id_named_graph = v.id_named_graph
                    WHERE get_bit(v.validity, %s) = 1
                """, requestedVersion);
        return jdbcTemplate.query(query,
                getRdfCompleteVersionedQuadRowMapper()
        );
    }

    public void saveAdd(
            Integer idSubject,
            Integer idProperty,
            Integer idObject,
            Integer idNamedGraph,
            Integer length
    ) {
        // FIXME : SQL injection !
        String query = String.format("""
                INSERT INTO versioned_quad (id_subject, id_property, id_object, id_named_graph, validity)
                VALUES (%s, %s, %s, %s, (
                SELECT LPAD('', %s, '0')::bit varying || B'1'
                ) )
                ON CONFLICT ON CONSTRAINT versioned_quad_pkey
                DO UPDATE SET validity = versioned_quad.validity || B'1';
                """, idSubject, idProperty, idObject, idNamedGraph, length);
        jdbcTemplate.execute(query);
    }

    public void saveRemove(
            Integer idSubject,
            Integer idProperty,
            Integer idObject,
            Integer idNamedGraph,
            Integer length
    ) {
        // FIXME : SQL injection !
        String query = String.format("""
                INSERT INTO versioned_quad (id_subject, id_property, id_object, id_named_graph, validity)
                VALUES (%s, %s, %s, %s, (
                SELECT LPAD('', %s, '0')::bit varying || B'0'
                ) )
                ON CONFLICT ON CONSTRAINT versioned_quad_pkey
                DO UPDATE SET validity = versioned_quad.validity || B'0';
                """, idSubject, idProperty, idObject, idNamedGraph, length);
        jdbcTemplate.execute(query);
    }

    private static RowMapper<RDFCompleteVersionedQuad> getRdfCompleteVersionedQuadRowMapper() {
        return (rs, rowNum) -> new RDFCompleteVersionedQuad(
                rs.getString(1),
                rs.getString(2),
                rs.getString(3),
                rs.getString(4),
                rs.getBytes(5)
        );
    }
}
