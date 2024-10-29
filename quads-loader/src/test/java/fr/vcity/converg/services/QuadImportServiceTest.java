package fr.vcity.converg.services;

import fr.vcity.converg.dto.CompleteVersionedQuad;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class QuadImportServiceTest {

    @Autowired
    private IQuadImportService quadImportService;

    @Autowired
    private IQueryService quadQueryService;

    @Test
    void resetDatabase() {
        quadImportService.resetDatabase();
        List<CompleteVersionedQuad> quads = quadQueryService.queryRequestedValidity("*");

        assertEquals(0, quads.size());
    }
}