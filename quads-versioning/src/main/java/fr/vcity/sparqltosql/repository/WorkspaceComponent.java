package fr.vcity.sparqltosql.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class WorkspaceComponent {
    private final JdbcTemplate jdbcTemplate;

    public WorkspaceComponent(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void saveTriples(String triplesQuery) {
        jdbcTemplate.execute("""
                WITH a (
                     subject, subject_type,
                     predicate, predicate_type,
                     object, object_type
                 ) AS (
                 VALUES""" + "\n" + triplesQuery +
                """
                )
                SELECT add_triple(
                    a.subject, a.subject_type,
                    a.predicate, a.predicate_type,
                    a.object, a.object_type
                ) FROM a;""");
    }
}
