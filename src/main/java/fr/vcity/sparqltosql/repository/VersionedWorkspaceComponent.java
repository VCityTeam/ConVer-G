package fr.vcity.sparqltosql.repository;

import fr.vcity.sparqltosql.dao.VersionedWorkspace;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class VersionedWorkspaceComponent {
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public VersionedWorkspaceComponent(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    public VersionedWorkspace save(
            Integer idSubject,
            Integer idProperty,
            Integer idObject,
            Integer length
    ) {
        return namedParameterJdbcTemplate.queryForObject("""
                        INSERT INTO versioned_workspace (id_subject, id_property, id_object, validity)
                        VALUES (:idSubject, :idProperty, :idObject, (
                        SELECT LPAD('', :length, '0')::bit varying || B'1'
                        ) )
                        ON CONFLICT ON CONSTRAINT versioned_workspace_pkey
                        DO UPDATE SET validity = versioned_workspace.validity || B'1'
                        RETURNING *;
                        """,
                new MapSqlParameterSource()
                        .addValue("idSubject", idSubject)
                        .addValue("idProperty", idProperty)
                        .addValue("idObject", idObject)
                        .addValue("length", length),
                (rs, i) -> new VersionedWorkspace(
                        rs.getInt("id_subject"),
                        rs.getInt("id_property"),
                        rs.getInt("id_object"),
                        rs.getBytes("validity")
                )
        );
    }
}
