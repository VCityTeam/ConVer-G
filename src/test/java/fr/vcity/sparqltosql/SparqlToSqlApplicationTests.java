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
    public void resetDatabase() {
        quadImportService.resetDatabase();
        List<RDFCompleteVersionedQuad> quads = quadQueryService.queryRequestedValidity("*");

        if (quads.size() > 0) {
            throw new RuntimeException("The result should be empty, current size: " + quads.size());
        }
    }

    @Test
    @Order(1)
    public void importQuadsAdd() throws Exception {
        Resource resource = resourceLoader.getResource("classpath:static/add/LYON_1ER_BATI_2015-add_bldg.nq");

        MockMultipartFile file
                = new MockMultipartFile(
                "files",
                resource.getFilename(),
                MediaType.TEXT_PLAIN_VALUE,
                resource.getInputStream().readAllBytes()
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("http://localhost:8080/import/add")
                        .file(file))
                .andExpect(status().isOk());

        List<RDFCompleteVersionedQuad> quads = quadQueryService.queryRequestedValidity("*");

        for (RDFCompleteVersionedQuad quad : quads) {
            assertEquals("1", new String(quad.getValidity(), StandardCharsets.UTF_8));
        }
    }

    @Test
    @Order(2)
    public void importQuadsRemove() throws Exception {
        Resource resource = resourceLoader.getResource("classpath:static/remove/LYON_1ER_BATI_2015-remove_bldg.nq");

        MockMultipartFile file
                = new MockMultipartFile(
                "files",
                resource.getFilename(),
                MediaType.TEXT_PLAIN_VALUE,
                resource.getInputStream().readAllBytes()
        );

        String hashParentVersion = quadQueryService.getHashOfVersion(1);

        mockMvc.perform(MockMvcRequestBuilders.multipart("http://localhost:8080/import/remove/" + hashParentVersion)
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

            assertEquals("10", new String(foundCVQ.getValidity(), StandardCharsets.UTF_8));
        }
    }

    @Test
    @Order(3)
    public void importQuadsRemoveAdd() throws Exception {
        Resource resource1 = resourceLoader.getResource("classpath:static/remove-add/LYON_1ER_BATI_2015-remove_bldg.nq");
        Resource resource2 = resourceLoader.getResource("classpath:static/remove-add/LYON_1ER_BATI_2015-add_bldg.nq");

        MockMultipartFile file1
                = new MockMultipartFile(
                "files",
                resource1.getFilename(),
                MediaType.TEXT_PLAIN_VALUE,
                resource1.getInputStream().readAllBytes()
        );

        MockMultipartFile file2
                = new MockMultipartFile(
                "files",
                resource2.getFilename(),
                MediaType.TEXT_PLAIN_VALUE,
                resource2.getInputStream().readAllBytes()
        );

        String hashParentVersion = quadQueryService.getHashOfVersion(1);

        mockMvc.perform(MockMvcRequestBuilders.multipart("http://localhost:8080/import/remove-add/" + hashParentVersion)
                        .file(file1)
                        .file(file2))
                .andExpect(status().isOk());

        List<RDFCompleteVersionedQuad> quads = quadQueryService.queryRequestedValidity("*");

        Dataset dataset1 =
                RDFParser.create()
                        .source(resource1.getInputStream())
                        .lang(RDFLanguages.nameToLang(FilenameUtils.getExtension(file1.getOriginalFilename())))
                        .errorHandler(ErrorHandlerFactory.errorHandlerStrict)
                        .toDataset();

        Dataset dataset2 =
                RDFParser.create()
                        .source(resource2.getInputStream())
                        .lang(RDFLanguages.nameToLang(FilenameUtils.getExtension(file2.getOriginalFilename())))
                        .errorHandler(ErrorHandlerFactory.errorHandlerStrict)
                        .toDataset();

        for (StmtIterator s = dataset1.getDefaultModel().listStatements(); s.hasNext();) {
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

            assertEquals("110", new String(foundCVQ.getValidity(), StandardCharsets.UTF_8));
        }

        for (StmtIterator s = dataset2.getDefaultModel().listStatements(); s.hasNext();) {
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

            assertEquals("101", new String(foundCVQ.getValidity(), StandardCharsets.UTF_8));
        }
    }

    @Test
    @Order(4)
    public void queryAllVersion() throws Exception {
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
    @Order(5)
    public void queryValidity() throws Exception {
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
    @Order(6)
    public void queryVersion() throws Exception {
        int index = 1;
        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get("http://localhost:8080/query/version/" + index))
                .andExpect(status().isOk())
                .andReturn();

        List<RDFCompleteVersionedQuad> resultQuads = List.of(new ObjectMapper().readValue(mvcResult.getResponse().getContentAsString(), RDFCompleteVersionedQuad[].class));

        for (RDFCompleteVersionedQuad quad : resultQuads) {
            assertEquals('1' , new String(quad.getValidity(), StandardCharsets.UTF_8).charAt(index));
        }
    }

    @Test
    @Order(7)
    public void getGraphVersion() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("http://localhost:8080/query/versions"))
                .andExpect(status().isOk())
                .andReturn();
        // TODO : Ajouter un test sur toutes les versions générées et le graphe associé
    }

    @Test
    @Order(8)
    public void querySPARQL() throws Exception {
        Resource resource = resourceLoader.getResource("classpath:static/queries/sparql.rq");
        mockMvc.perform(MockMvcRequestBuilders.multipart("http://localhost:8080/query/sparql")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8)))
                .andExpect(status().isOk())
                .andReturn();
        // TODO : Ajouter les tests sur le résultat retourné lorsque le parser SPARQL sera réalisé
    }
}
