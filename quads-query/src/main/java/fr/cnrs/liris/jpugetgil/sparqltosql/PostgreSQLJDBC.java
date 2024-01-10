package fr.cnrs.liris.jpugetgil.sparqltosql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


public class PostgreSQLJDBC {

    private static final Logger log = LoggerFactory.getLogger(App.class);

    private final String url = "jdbc:postgresql://localhost/sparqltosql";
    private final String user = "postgres";
    private final String password = "password";

    Connection conn;

    public PostgreSQLJDBC() {
        try {
            this.conn = DriverManager.getConnection(url, user, password);
            log.info("Connected to the PostgreSQL server successfully.");
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
    }

    public void doSelect() {
        try {
            conn.setAutoCommit(false);
            conn.createStatement().execute("""
                SELECT * FROM resource_or_literal LIMIT 10;
            """);
            conn.commit();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
}

