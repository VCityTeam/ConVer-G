package fr.cnrs.liris.jpugetgil.converg.connection;

import org.postgresql.PGProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Objects;
import java.util.Properties;

/**
 * Created by jpugetgil.
 * Singleton for the JDBC session
 */
public class JdbcConnection {

    private static final Logger log = LoggerFactory.getLogger(JdbcConnection.class);

    private static JdbcConnection jdbcConnection;
    private Connection connection;
    private Statement statement;
    private static final String CONNECTION_URL = System.getenv("DATASOURCE_URL") == null ?
            "jdbc:postgresql://localhost:5432/converg" : System.getenv("DATASOURCE_URL");
    private static final String CONNECTION_USERNAME = System.getenv("DATASOURCE_USERNAME") == null ?
            "postgres" : System.getenv("DATASOURCE_USERNAME");
    private static final String CONNECTION_PASSWORD = System.getenv("DATASOURCE_PASSWORD") == null ?
            "password" : System.getenv("DATASOURCE_PASSWORD");

    /**
     * Constructor method in order to create db connection & statement
     *
     * @throws SQLException exception can be thrown during DB transaction
     */
    private JdbcConnection() throws SQLException {
        try {
            Properties connectionProperties = new Properties();
            connectionProperties.setProperty(PGProperty.USER.getName(), CONNECTION_USERNAME);
            connectionProperties.setProperty(PGProperty.PASSWORD.getName(), CONNECTION_PASSWORD);
            connectionProperties.setProperty(PGProperty.CONNECT_TIMEOUT.getName(), "120");

            connection = DriverManager.getConnection(CONNECTION_URL, connectionProperties);
            log.info("Connection established successfully with the database");
            log.info("Connection URL: {}", CONNECTION_URL);
            statement = connection.createStatement();
        } catch (SQLException exception) {
            log.error(exception.getMessage());
        } finally {
            if (connection != null && Objects.isNull(statement)) {
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

    /**
     * Execute the given SQL query
     *
     * @param sqlQuery SQL query to be executed
     * @return ResultSet result of the SQL query
     * @throws SQLException exception can be thrown during DB transaction
     */
    public ResultSet executeSQL(String sqlQuery) throws SQLException {
        return statement.executeQuery(sqlQuery);
    }
}