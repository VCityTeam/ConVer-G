package fr.vcity.sparqltosql;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.vcity.sparqltosql.dto.RDFCompleteVersionedQuad;
import fr.vcity.sparqltosql.services.IQuadImportService;
import fr.vcity.sparqltosql.services.IQueryService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SparqlToSqlApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private IQuadImportService quadImportService;

    @Autowired
    private IQueryService quadQueryService;

    @Test
    @Order(0)
    void resetDatabase() {
        quadImportService.resetDatabase();
        List<RDFCompleteVersionedQuad> quads = quadQueryService.queryRequestedValidity("*");

        assertEquals(0, quads.size());
    }

    @Test
    @Order(1)
    void importV1() throws Exception {
        Resource resource = resourceLoader.getResource("classpath:dataset/quads/GratteCiel_2018_split.ttl.quads_named_graph.nq");

        MockMultipartFile file
                = new MockMultipartFile(
                "file",
                resource.getFilename(),
                MediaType.TEXT_PLAIN_VALUE,
                resource.getInputStream().readAllBytes()
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("http://localhost:8080/import/version")
                        .file(file))
                .andExpect(status().isOk());

        List<RDFCompleteVersionedQuad> quads = quadQueryService.queryRequestedValidity("*");

        for (RDFCompleteVersionedQuad quad : quads) {
            assertEquals("1", new String(quad.getValidity(), StandardCharsets.UTF_8));
        }
    }

    @Test
    @Order(2)
    void importV2() throws Exception {
        Resource resource = resourceLoader.getResource("classpath:dataset/quads/GratteCiel_2015_split.ttl.quads_named_graph.nq");

        MockMultipartFile file
                = new MockMultipartFile(
                "file",
                resource.getFilename(),
                MediaType.TEXT_PLAIN_VALUE,
                resource.getInputStream().readAllBytes()
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("http://localhost:8080/import/version")
                        .file(file))
                .andExpect(status().isOk());

        List<RDFCompleteVersionedQuad> quads = quadQueryService.queryRequestedValidity("*");

        for (RDFCompleteVersionedQuad quad : quads) {
            assertEquals("https://github.com/VCityTeam/SPARQL-to-SQL/GraphName#Villeurbanne", quad.getNamedGraph());
            assertNotNull(quad.getS());
            assertNotNull(quad.getP());
            assertNotNull(quad.getO());
        }
    }

    @Test
    @Order(3)
    void queryAllVersion() throws Exception {
        String validity = "*";
        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get("http://localhost:8080/query/validity/" + validity))
                .andExpect(status().isOk())
                .andReturn();

        List<RDFCompleteVersionedQuad> resultQuads = List.of(new ObjectMapper().readValue(mvcResult.getResponse().getContentAsString(), RDFCompleteVersionedQuad[].class));

        for (RDFCompleteVersionedQuad resultQuad : resultQuads) {
            assertNotNull(resultQuad.getS());
            assertNotNull(resultQuad.getP());
            assertNotNull(resultQuad.getO());
            assertNotNull(resultQuad.getNamedGraph());
            assertNotEquals("", new String(resultQuad.getValidity(), StandardCharsets.UTF_8));
        }
    }

    @Test
    @Order(4)
    void queryValidity() throws Exception {
        String validity = "10";
        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get("http://localhost:8080/query/validity/" + validity))
                .andExpect(status().isOk())
                .andReturn();

        List<RDFCompleteVersionedQuad> resultQuads = List.of(new ObjectMapper().readValue(mvcResult.getResponse().getContentAsString(), RDFCompleteVersionedQuad[].class));

        for (RDFCompleteVersionedQuad resultQuad : resultQuads) {
            assertNotNull(resultQuad.getS());
            assertNotNull(resultQuad.getP());
            assertNotNull(resultQuad.getO());
            assertNotNull(resultQuad.getNamedGraph());
            assertEquals("10", new String(resultQuad.getValidity(), StandardCharsets.UTF_8));
        }
    }

    @Test
    @Order(5)
    void queryVersion() throws Exception {
        int index = 1;
        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get("http://localhost:8080/query/version/" + index))
                .andExpect(status().isOk())
                .andReturn();

        List<RDFCompleteVersionedQuad> resultQuads = List.of(new ObjectMapper().readValue(mvcResult.getResponse().getContentAsString(), RDFCompleteVersionedQuad[].class));

        for (RDFCompleteVersionedQuad quad : resultQuads) {
            assertEquals('1', new String(quad.getValidity(), StandardCharsets.UTF_8).charAt(index));
        }
    }

    @Test
    @Order(6)
    void importWorkspace() throws Exception {
        Resource resource = resourceLoader.getResource("classpath:dataset/quads/GratteCiel_2009_2018_Workspace.ttl.quads_named_graph.nq");
        Resource resource2 = resourceLoader.getResource("classpath:dataset/quads/Transition_2015_2018.ttl.quads_named_graph.nq");

        MockMultipartFile file
                = new MockMultipartFile(
                "file",
                resource.getFilename(),
                MediaType.TEXT_PLAIN_VALUE,
                resource.getInputStream().readAllBytes()
        );

        MockMultipartFile file2
                = new MockMultipartFile(
                "file",
                resource2.getFilename(),
                MediaType.TEXT_PLAIN_VALUE,
                resource2.getInputStream().readAllBytes()
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("http://localhost:8080/import/workspace")
                        .file(file))
                .andExpect(status().isOk());

        mockMvc.perform(MockMvcRequestBuilders.multipart("http://localhost:8080/import/workspace")
                        .file(file2))
                .andExpect(status().isOk());
    }

    @Test
    @Order(7)
    void getGraphVersion() throws Exception {
        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get("http://localhost:8080/query/versions"))
                .andExpect(status().isOk())
                .andReturn();

        assertNotNull(mvcResult.getResponse().getContentAsString());
    }

    @Test
    @Order(8)
    void removeWorkspace() throws Exception {
        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.delete("http://localhost:8080/import/workspace"))
                .andExpect(status().isOk())
                .andReturn();

        assertNotNull(mvcResult.getResponse().getContentAsString());
    }

    @Test
    @Order(9)
    void sendCorruptedFileVersion() throws Exception {
        MockMultipartFile file
                = new MockMultipartFile(
                "file",
                "Corrupted_File.ttl.quads_named_graph.nq",
                MediaType.TEXT_PLAIN_VALUE,
                new byte[]{0}
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("http://localhost:8080/import/version")
                        .file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(10)
    void sendCorruptedFileWorkspace() throws Exception {
        MockMultipartFile file
                = new MockMultipartFile(
                "file",
                "Corrupted_File.ttl.quads_named_graph.nq",
                MediaType.TEXT_PLAIN_VALUE,
                new byte[]{0, 1}
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("http://localhost:8080/import/workspace")
                        .file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(11)
    void querySPARQL() throws Exception {
        Resource resource = resourceLoader.getResource("classpath:queries/sparql.rq");
        mockMvc.perform(MockMvcRequestBuilders.multipart("http://localhost:8080/query/sparql")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8)))
                .andExpect(status().isOk())
                .andReturn();
        // TODO : Ajouter les tests sur le résultat retourné lorsque le parser SPARQL sera réalisé
    }
}
