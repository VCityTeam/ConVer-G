package fr.vcity.sparqltosql;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.vcity.sparqltosql.dto.RDFCompleteVersionedQuad;
import fr.vcity.sparqltosql.services.IQuadImportService;
import fr.vcity.sparqltosql.services.IQuadQueryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.system.ErrorHandlerFactory;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    private IQuadQueryService quadQueryService;

    @Test
    @Order(0)
    void resetDatabase() {
        quadImportService.resetDatabase();
        List<RDFCompleteVersionedQuad> quads = quadQueryService.queryRequestedValidity("*");

        assertEquals(0, quads.size());
    }

    @Test
    @Order(1)
    void importQuadsV1() throws Exception {
        Resource resource = resourceLoader.getResource("classpath:static/dataset/LYON_1ER_BATI_2015-1_bldg.nq");

        MockMultipartFile file
                = new MockMultipartFile(
                "files",
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
    void importQuadsV2() throws Exception {
        Resource resource = resourceLoader.getResource("classpath:static/dataset/LYON_1ER_BATI_2015-2_bldg.nq");

        MockMultipartFile file
                = new MockMultipartFile(
                "files",
                resource.getFilename(),
                MediaType.TEXT_PLAIN_VALUE,
                resource.getInputStream().readAllBytes()
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("http://localhost:8080/import/version" )
                        .file(file))
                .andExpect(status().isOk());

        List<RDFCompleteVersionedQuad> quads = quadQueryService.queryRequestedValidity("*");

        Dataset dataset =
                RDFParser.create()
                        .source(resource.getInputStream())
                        .lang(RDFLanguages.nameToLang(FilenameUtils.getExtension(file.getOriginalFilename())))
                        .errorHandler(ErrorHandlerFactory.errorHandlerStrict)
                        .toDataset();

        for (StmtIterator s = dataset.getDefaultModel().listStatements(); s.hasNext();) {
            Statement statement = s.nextStatement();
            RDFCompleteVersionedQuad foundCVQ = quads
                    .stream()
                    .filter(rdfCVQ ->
                            rdfCVQ.getS().equals(statement.getSubject().getURI()) &&
                                    rdfCVQ.getP().equals(statement.getPredicate().getURI()) &&
                                    rdfCVQ.getO().equals(statement.getObject().toString()) &&
                                    rdfCVQ.getNamedGraph().equals("default")
                    )
                    .findFirst()
                    .orElseThrow();

            assertEquals("11", new String(foundCVQ.getValidity(), StandardCharsets.UTF_8));
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

        List<RDFCompleteVersionedQuad> quads = quadQueryService.queryRequestedValidity(validity);

        for (int index = 0; index < quads.size(); index++) {
            assertEquals(resultQuads.get(index).getS(), quads.get(index).getS());
            assertEquals(resultQuads.get(index).getP(), quads.get(index).getP());
            assertEquals(resultQuads.get(index).getO(), quads.get(index).getO());
            assertEquals(resultQuads.get(index).getNamedGraph(), quads.get(index).getNamedGraph());
            assertEquals(new String(resultQuads.get(index).getValidity(), StandardCharsets.UTF_8), new String(quads.get(index).getValidity(), StandardCharsets.UTF_8));
        }
    }

    @Test
    @Order(4)
    void queryValidity() throws Exception {
        String validity = "101";
        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get("http://localhost:8080/query/validity/" + validity))
                .andExpect(status().isOk())
                .andReturn();

        List<RDFCompleteVersionedQuad> resultQuads = List.of(new ObjectMapper().readValue(mvcResult.getResponse().getContentAsString(), RDFCompleteVersionedQuad[].class));

        List<RDFCompleteVersionedQuad> quads = quadQueryService.queryRequestedValidity(validity);

        for (int index = 0; index < quads.size(); index++) {
            assertEquals(resultQuads.get(index).getS(), quads.get(index).getS());
            assertEquals(resultQuads.get(index).getP(), quads.get(index).getP());
            assertEquals(resultQuads.get(index).getO(), quads.get(index).getO());
            assertEquals(resultQuads.get(index).getNamedGraph(), quads.get(index).getNamedGraph());
            assertEquals(new String(resultQuads.get(index).getValidity(), StandardCharsets.UTF_8), new String(quads.get(index).getValidity(), StandardCharsets.UTF_8));
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
            assertEquals('1' , new String(quad.getValidity(), StandardCharsets.UTF_8).charAt(index));
        }
    }

//    @Test
//    @Order(6)
//    void getGraphVersion() throws Exception {
//        mockMvc.perform(MockMvcRequestBuilders.get("http://localhost:8080/query/versions"))
//                .andExpect(status().isOk())
//                .andReturn();
//        // TODO : Ajouter un test sur toutes les versions générées et le graphe associé
//    }

    @Test
    @Order(7)
    void querySPARQL() throws Exception {
        Resource resource = resourceLoader.getResource("classpath:static/queries/sparql.rq");
        mockMvc.perform(MockMvcRequestBuilders.multipart("http://localhost:8080/query/sparql")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8)))
                .andExpect(status().isOk())
                .andReturn();
        // TODO : Ajouter les tests sur le résultat retourné lorsque le parser SPARQL sera réalisé
    }
}
