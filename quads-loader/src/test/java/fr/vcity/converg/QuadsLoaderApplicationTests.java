package fr.vcity.converg;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.vcity.converg.dto.CompleteVersionedQuad;
import fr.vcity.converg.services.IQuadImportService;
import fr.vcity.converg.services.IQueryService;
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
class QuadsLoaderApplicationTests {

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
        List<CompleteVersionedQuad> quads = quadQueryService.queryRequestedValidity("*");

        assertEquals(0, quads.size());
    }

    /**
     * Tests the import of the first version and check if the validity is set to 1
     *
     * @throws Exception a input stream or MVC exception
     */
    @Test
    @Order(1)
    void importV1() throws Exception {
        Resource resource = resourceLoader.getResource("classpath:dataset/version-1.split.ttl.relational.nq");

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

        List<CompleteVersionedQuad> quads = quadQueryService.queryRequestedValidity("*");

        for (CompleteVersionedQuad quad : quads) {
            assertEquals("1", new String(quad.getValidity(), StandardCharsets.UTF_8));
        }
    }

    /**
     * Tests the import of a second version and check the metadata and the data is well annotated
     *
     * @throws Exception a input stream or MVC exception
     */
    @Test
    @Order(2)
    void importV2() throws Exception {
        Resource resource = resourceLoader.getResource("classpath:dataset/version-2.split.ttl.relational.nq");

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

        List<CompleteVersionedQuad> quads = quadQueryService.queryRequestedValidity("*");

        for (CompleteVersionedQuad quad : quads) {
            if (quad.getS().equals("https://github.com/VCityTeam/ConVer-G/Named-Graph#BSBM")) {
                assertEquals("default", quad.getNamedGraph());
            } else {
                assertEquals("https://github.com/VCityTeam/ConVer-G/Named-Graph#BSBM", quad.getNamedGraph());
            }
            assertNotNull(quad.getS());
            assertNotNull(quad.getP());
            assertNotNull(quad.getO());
        }
    }

    /**
     * Tests the query of all the versions and check that the data is not empty
     *
     * @throws Exception a MVC exception
     */
    @Test
    @Order(3)
    void queryAllVersion() throws Exception {
        String validity = "*";
        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get("http://localhost:8080/query/validity/" + validity))
                .andExpect(status().isOk())
                .andReturn();

        List<CompleteVersionedQuad> resultQuads = List.of(new ObjectMapper().readValue(mvcResult.getResponse().getContentAsString(), CompleteVersionedQuad[].class));

        for (CompleteVersionedQuad resultQuad : resultQuads) {
            assertNotNull(resultQuad.getS());
            assertNotNull(resultQuad.getP());
            assertNotNull(resultQuad.getO());
            assertNotNull(resultQuad.getNamedGraph());
            assertNotEquals("", new String(resultQuad.getValidity(), StandardCharsets.UTF_8));
        }
    }

    /**
     * Tests the query of a specific validity pattern and check the associated data and validity
     *
     * @throws Exception a MVC exception
     */
    @Test
    @Order(4)
    void queryValidity() throws Exception {
        String validity = "10";
        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get("http://localhost:8080/query/validity/" + validity))
                .andExpect(status().isOk())
                .andReturn();

        List<CompleteVersionedQuad> resultQuads = List.of(new ObjectMapper().readValue(mvcResult.getResponse().getContentAsString(), CompleteVersionedQuad[].class));

        for (CompleteVersionedQuad resultQuad : resultQuads) {
            assertNotNull(resultQuad.getS());
            assertNotNull(resultQuad.getP());
            assertNotNull(resultQuad.getO());
            assertNotNull(resultQuad.getNamedGraph());
            assertEquals("10", new String(resultQuad.getValidity(), StandardCharsets.UTF_8));
        }
    }

    /**
     * Tests the query of all the versions and check that the data is not empty
     *
     * @throws Exception a MVC exception
     */
    @Test
    @Order(5)
    void queryVersion() throws Exception {
        int index = 1;
        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get("http://localhost:8080/query/version/" + index))
                .andExpect(status().isOk())
                .andReturn();

        List<CompleteVersionedQuad> resultQuads = List.of(new ObjectMapper().readValue(mvcResult.getResponse().getContentAsString(), CompleteVersionedQuad[].class));

        for (CompleteVersionedQuad quad : resultQuads) {
            assertEquals('1', new String(quad.getValidity(), StandardCharsets.UTF_8).charAt(index));
        }
    }

//    /**
//     * Tests the import of the metadata
//     *
//     * @throws Exception a input stream or MVC exception
//     */
//    @Test
//    @Order(6)
//    void importMetadata() throws Exception {
//        Resource resource = resourceLoader.getResource("classpath:dataset/transition-0.nt.relational.nq");
//
//        MockMultipartFile file1
//                = new MockMultipartFile(
//                "file",
//                resource.getFilename(),
//                MediaType.TEXT_PLAIN_VALUE,
//                resource.getInputStream().readAllBytes()
//        );
//
//        mockMvc.perform(MockMvcRequestBuilders.multipart("http://localhost:8080/import/metadata")
//                        .file(file1))
//                .andExpect(status().isOk());
//    }

//    @Test
//    @Order(7)
//    void getGraphVersion() throws Exception {
//        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get("http://localhost:8080/query/versions"))
//                .andExpect(status().isOk())
//                .andReturn();
//
//        assertNotNull(mvcResult.getResponse().getContentAsString());
//    }

    @Test
    @Order(8)
    void removeMetadata() throws Exception {
        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.delete("http://localhost:8080/import/metadata"))
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
                "Corrupted_File.ttl.relational.nq",
                MediaType.TEXT_PLAIN_VALUE,
                new byte[]{0}
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("http://localhost:8080/import/version")
                        .file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(10)
    void sendCorruptedFileMetadata() throws Exception {
        MockMultipartFile file
                = new MockMultipartFile(
                "file",
                "Corrupted_File.ttl.relational.nq",
                MediaType.TEXT_PLAIN_VALUE,
                new byte[]{0, 1}
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("http://localhost:8080/import/metadata")
                        .file(file))
                .andExpect(status().isBadRequest());
    }

//    /**
//     * Tests the import of the metadata
//     *
//     * @throws Exception a input stream or MVC exception
//     */
//    @Test
//    @Order(11)
//    void importMetadata2() throws Exception {
//        Resource resource = resourceLoader.getResource("classpath:dataset/transition-0.nt.relational.nq");
//        Resource resource2 = resourceLoader.getResource("classpath:dataset/transition-1.nt.relational.nq");
//
//        MockMultipartFile file1
//                = new MockMultipartFile(
//                "file",
//                resource.getFilename(),
//                MediaType.TEXT_PLAIN_VALUE,
//                resource.getInputStream().readAllBytes()
//        );
//
//        MockMultipartFile file2
//                = new MockMultipartFile(
//                "file",
//                resource2.getFilename(),
//                MediaType.TEXT_PLAIN_VALUE,
//                resource2.getInputStream().readAllBytes()
//        );
//
//        mockMvc.perform(MockMvcRequestBuilders.multipart("http://localhost:8080/import/metadata")
//                        .file(file1))
//                .andExpect(status().isOk());
//
//        mockMvc.perform(MockMvcRequestBuilders.multipart("http://localhost:8080/import/metadata")
//                        .file(file2))
//                .andExpect(status().isOk());
//    }
}
