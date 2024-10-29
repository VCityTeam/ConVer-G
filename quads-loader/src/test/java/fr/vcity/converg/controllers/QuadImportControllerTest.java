package fr.vcity.converg.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class QuadImportControllerTest {

    @Autowired
    QuadImportController quadImportController;

    @Test
    void contextLoads() {
        assertThat(quadImportController).isNotNull();
    }
}