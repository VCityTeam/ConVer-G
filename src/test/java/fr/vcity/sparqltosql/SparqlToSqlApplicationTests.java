package fr.vcity.sparqltosql;

import fr.vcity.sparqltosql.services.IQuadQueryService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
class SparqlToSqlApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private IQuadQueryService quadQueryService;

    @Test
    @Order(1)
    public void resetDatabase() {
        log.info("Cleaning database...");
        quadQueryService.resetDatabase();
    }

    @Test
    @Order(2)
    public void importQuads() throws Exception {
        Resource resource = resourceLoader.getResource("classpath:static/LYON_1ER_BATI_2015-1_bldg.nq");
        String content = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);

        log.info("File content: {}", content);

        mockMvc.perform(MockMvcRequestBuilders.post("http://localhost:8080/quads/import/N-QUADS")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(content))
                .andExpect(status().isOk());
    }

}
