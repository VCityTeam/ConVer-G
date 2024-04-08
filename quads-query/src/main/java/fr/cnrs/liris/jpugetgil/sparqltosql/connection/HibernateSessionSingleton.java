package fr.cnrs.liris.jpugetgil.sparqltosql.connection;

import fr.cnrs.liris.jpugetgil.sparqltosql.dao.*;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Created by jpugetgil.
 * Singleton for the hibernate session
 */
public class HibernateSessionSingleton {

    private static final String CONNECTION_URL = System.getenv("DATASOURCE_URL") == null ?
            "jdbc:postgresql://localhost:5432/sparqltosql" : System.getenv("DATASOURCE_URL");
    private static final String CONNECTION_USERNAME = System.getenv("DATASOURCE_USERNAME") == null ?
            "postgres" : System.getenv("DATASOURCE_USERNAME");
    private static final String CONNECTION_PASSWORD = System.getenv("DATASOURCE_PASSWORD") == null ?
            "password" : System.getenv("DATASOURCE_PASSWORD");

    private static HibernateSessionSingleton instance;

    private static final Logger log = LoggerFactory.getLogger(HibernateSessionSingleton.class);

    private final SessionFactory sessionFactory;

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public static HibernateSessionSingleton getInstance() {
        if (instance == null) {
            instance = new HibernateSessionSingleton();
        }
        return instance;
    }


    private HibernateSessionSingleton() {
        log.info("Creation of the session...");

        Properties props = new Properties();
        props.put("hibernate.connection.url", CONNECTION_URL);
        props.put("hibernate.connection.username", CONNECTION_USERNAME);
        props.put("hibernate.connection.password", CONNECTION_PASSWORD);
        props.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        props.put("hibernate.connection.driver_class", "org.postgresql.Driver");
        props.put("hibernate.show_sql", "true");
        props.put("hibernate.format_sql", "true");

        Configuration configuration = new Configuration()
                .addAnnotatedClass(ResourceOrLiteral.class)
                .addAnnotatedClass(VersionedQuad.class)
                .addAnnotatedClass(VersionedNamedGraph.class)
                .addAnnotatedClass(Version.class)
                .addAnnotatedClass(Workspace.class)
                .setProperties(props);

        ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
                .applySettings(configuration.getProperties())
                .build();

        sessionFactory = configuration.buildSessionFactory(serviceRegistry);
    }
}

