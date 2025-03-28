package fr.vcity.converg.connection;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Objects;

/**
 * Created by jpugetgil.
 * Singleton for the JDBC session
 */
public class JdbcConnection {

    private static final Logger log = LoggerFactory.getLogger(JdbcConnection.class);

    private static JdbcConnection jdbcConnection;
    @Getter
    private Connection connection;
    private Statement statement;
    private static final String CONNECTION_URL = System.getenv("SPRING_DATASOURCE_URL") == null ?
            "jdbc:postgresql://localhost:5432/converg" : System.getenv("SPRING_DATASOURCE_URL");
    private static final String CONNECTION_USERNAME = System.getenv("SPRING_DATASOURCE_USERNAME") == null ?
            "postgres" : System.getenv("SPRING_DATASOURCE_USERNAME");
    private static final String CONNECTION_PASSWORD = System.getenv("SPRING_DATASOURCE_PASSWORD") == null ?
            "password" : System.getenv("SPRING_DATASOURCE_PASSWORD");

    /**
     * Constructor method in order to create db connection & statement
     *
     * @throws SQLException exception can be thrown during DB transaction
     */
    private JdbcConnection() throws SQLException {
        try {
            connection = DriverManager.getConnection(CONNECTION_URL, CONNECTION_USERNAME, CONNECTION_PASSWORD);
            log.info("Connection established successfully with the database");
            log.info("Connection URL: {}", CONNECTION_URL);
            statement = connection.createStatement();
        } catch (SQLException exception) {
            log.error(exception.getMessage());
        } finally {
            if (Objects.isNull(statement)) {
                assert connection != null;
                connection.close();
            }
        }
    }

    /**
     * Create the instance of {@link JdbcConnection} object if it is not created yet and guarantee that there is only one single instance is created for this class.
     *
     * @return JdbcConnection created single instance
     */
    public static JdbcConnection getInstance() {
        try {
            if (Objects.isNull(jdbcConnection))
                jdbcConnection = new JdbcConnection();
        } catch (SQLException exception) {
            log.error(exception.getMessage());
        }
        return jdbcConnection;
    }
}