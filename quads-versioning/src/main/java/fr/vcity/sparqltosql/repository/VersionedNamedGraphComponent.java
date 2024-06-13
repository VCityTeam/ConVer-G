package fr.vcity.sparqltosql.repository;

import fr.vcity.sparqltosql.connection.JdbcConnection;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Slf4j
@Component
public class VersionedNamedGraphComponent {

    public void saveVersionedNamedGraph(List<String> namedGraphs, String originalFilename, Integer indexVersion) {
        JdbcConnection jdbcConnection = JdbcConnection.getInstance();
        Connection connection = jdbcConnection.getConnection();

        for (List<String> partition : ListUtils.partition(namedGraphs, 100)) {
            String insertNamedGraphSQL = """
                    WITH a (
                        named_graph,
                        filename,
                        version
                    ) AS (""" + "VALUES (?,?,?)" + """
                    )
                    SELECT version_named_graph(
                        a.named_graph,
                        a.filename,
                        a.version
                    ) FROM a;""";
            try {
                PreparedStatement ps = connection.prepareStatement(insertNamedGraphSQL);
                for (String namedGraph : partition) {
                    ps.setString(1, namedGraph);
                    ps.setString(2, originalFilename);
                    ps.setInt(3, indexVersion);
                    ps.addBatch();
                }

                ps.executeBatch();
            } catch (SQLException e) {
                log.error("Error occurred in statement", e);
            }
        }
    }
}
