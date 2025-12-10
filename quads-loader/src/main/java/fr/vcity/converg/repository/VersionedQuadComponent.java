package fr.vcity.converg.repository;

import fr.vcity.converg.dto.CompleteVersionedQuad;
import fr.vcity.converg.services.QuadImportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Component
@Slf4j
public class VersionedQuadComponent {
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final DataSource dataSource;

    public VersionedQuadComponent(NamedParameterJdbcTemplate namedParameterJdbcTemplate, DataSource dataSource) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.dataSource = dataSource;
    }

    public List<CompleteVersionedQuad> findAll() {
        return namedParameterJdbcTemplate.query("""
                        SELECT rls.name, rlp.name, rlo.name, rlong.name, v.validity
                            FROM versioned_quad v LEFT JOIN resource_or_literal rls ON rls.id_resource_or_literal = v.id_subject
                            LEFT JOIN resource_or_literal rlp ON rlp.id_resource_or_literal = v.id_predicate
                            LEFT JOIN resource_or_literal rlo ON rlo.id_resource_or_literal = v.id_object
                            LEFT JOIN resource_or_literal rlong ON rlong.id_resource_or_literal = v.id_named_graph""",
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

    public void flattenVersionedQuads() {
        try (Connection connection = dataSource.getConnection()) {
            String insertFlatQuadSQL = """
                    INSERT INTO versioned_quad_flat (id_subject, id_predicate, id_object, id_versioned_named_graph)
                    SELECT v.id_subject, v.id_predicate, v.id_object, vng.id_versioned_named_graph
                    FROM versioned_quad v
                    JOIN versioned_named_graph vng ON get_bit(v.validity, vng.index_version - 1) = 1
                        AND vng.id_named_graph = v.id_named_graph;""";
            try {
                PreparedStatement ps = connection.prepareStatement(insertFlatQuadSQL);
                ps.executeUpdate();
            } catch (SQLException e) {
                log.error("Error occurred in statement", e);
                throw new RuntimeException("Failed to flatten versioned quads", e);
            }
        } catch (SQLException e) {
            log.error("Error getting connection", e);
            throw new RuntimeException("Failed to get database connection", e);
        }
    }

    public void saveQuads(List<QuadImportService.QuadValueType> quadValueTypes) {
        try (Connection connection = dataSource.getConnection()) {
            String insertQuadValueSQL = """
                    INSERT INTO flat_model_quad (subject, subject_type, predicate, predicate_type, object, object_type, named_graph, version)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?);""";

            try {
                PreparedStatement ps = connection.prepareStatement(insertQuadValueSQL);

                for (QuadImportService.QuadValueType quadValueType : quadValueTypes) {
                    ps.setString(1, removeAllSpecialCharacters(quadValueType.tripleValueType().sValue()));
                    ps.setString(2, removeAllSpecialCharacters(quadValueType.tripleValueType().sType()));
                    ps.setString(3, removeAllSpecialCharacters(quadValueType.tripleValueType().pValue()));
                    ps.setString(4, removeAllSpecialCharacters(quadValueType.tripleValueType().pType()));
                    ps.setString(5, removeAllSpecialCharacters(quadValueType.tripleValueType().oValue()));
                    ps.setString(6, removeAllSpecialCharacters(quadValueType.tripleValueType().oType()));
                    ps.setString(7, removeAllSpecialCharacters(quadValueType.namedGraph()));
                    ps.setInt(8, quadValueType.version());
                    ps.addBatch();
                }

                ps.executeBatch();
            } catch (SQLException e) {
                log.error("Error occurred in statement", e);
                throw new RuntimeException("Failed to save quads", e);
            }
        } catch (SQLException e) {
            log.error("Error getting connection", e);
            throw new RuntimeException("Failed to get database connection", e);
        }
    }

    public String removeAllSpecialCharacters(String input) {
        if (input == null) {
            return null;
        }
        return input
            .replace("\n", " ")
            .replace("\r", " ")
            .replace("\t", " ")
            .replace("\\", "\\\\");
    }
}
