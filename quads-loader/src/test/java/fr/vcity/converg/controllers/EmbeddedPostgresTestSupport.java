package fr.vcity.converg.controllers;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;

abstract class EmbeddedPostgresTestSupport {

    private static final int POSTGRES_PORT = 5433;

    private static final EmbeddedPostgres EMBEDDED_POSTGRES = startEmbeddedPostgres();

    @BeforeAll
    static void initializeEmbeddedDatabase() {
        // The static initializer starts the database before Spring creates the context.
    }

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:postgresql://localhost:" + POSTGRES_PORT + "/postgres");
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    private static EmbeddedPostgres startEmbeddedPostgres() {
        try {
            return EmbeddedPostgres.builder()
                    .setPort(POSTGRES_PORT)
                    .start();
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }
}