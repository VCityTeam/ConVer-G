package fr.cnrs.liris.jpugetgil.sparqltosql;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test for SPARQL queries.
 */
public class AppTest {
    private static final Logger log = LoggerFactory.getLogger(AppTest.class);

    @Test
    @Order(1)
    void querySPARQL0() throws Exception {
        Path path = Path.of("src/test/resources/queries/sparql-0.rq");
        HttpRequest requestStS = getHttpRequestByURLandPath("http://localhost:8081/rdf/query", path);
        HttpRequest requestBlazegraph = getHttpRequestByURLandPath("http://localhost:9999/blazegraph/namespace/kb/sparql", path);

        sendRequestAndCompareResults(requestStS, requestBlazegraph);
    }

    @Test
    @Order(2)
    void querySPARQL1() throws Exception {
        Path path = Path.of("src/test/resources/queries/sparql-1.rq");
        HttpRequest requestStS = getHttpRequestByURLandPath("http://localhost:8081/rdf/query", path);
        HttpRequest requestBlazegraph = getHttpRequestByURLandPath("http://localhost:9999/blazegraph/namespace/kb/sparql", path);

        sendRequestAndCompareResults(requestStS, requestBlazegraph);
    }

    @Test
    @Order(3)
    void querySPARQL2() throws Exception {
        Path path = Path.of("src/test/resources/queries/sparql-2.rq");
        HttpRequest requestStS = getHttpRequestByURLandPath("http://localhost:8081/rdf/query", path);
        HttpRequest requestBlazegraph = getHttpRequestByURLandPath("http://localhost:9999/blazegraph/namespace/kb/sparql", path);

        sendRequestAndCompareResults(requestStS, requestBlazegraph);
    }

    @Test
    @Order(4)
    void querySPARQL3() throws Exception {
        Path path = Path.of("src/test/resources/queries/sparql-3.rq");
        HttpRequest requestStS = getHttpRequestByURLandPath("http://localhost:8081/rdf/query", path);
        HttpRequest requestBlazegraph = getHttpRequestByURLandPath("http://localhost:9999/blazegraph/namespace/kb/sparql", path);

        sendRequestAndCompareResults(requestStS, requestBlazegraph);
    }

    @Test
    @Order(5)
    void querySPARQL4() throws Exception {
        Path path = Path.of("src/test/resources/queries/sparql-4.rq");
        HttpRequest requestStS = getHttpRequestByURLandPath("http://localhost:8081/rdf/query", path);
        HttpRequest requestBlazegraph = getHttpRequestByURLandPath("http://localhost:9999/blazegraph/namespace/kb/sparql", path);

        sendRequestAndCompareResults(requestStS, requestBlazegraph);
    }

    @Test
    @Order(6)
    void querySPARQL5() throws Exception {
        Path path = Path.of("src/test/resources/queries/sparql-4.rq");
        HttpRequest requestStS = getHttpRequestByURLandPath("http://localhost:8081/rdf/query", path);
        HttpRequest requestBlazegraph = getHttpRequestByURLandPath("http://localhost:9999/blazegraph/namespace/kb/sparql", path);

        sendRequestAndCompareResults(requestStS, requestBlazegraph);
    }

    /**
     * Send the request to the two endpoints and compare the results.
     *
     * @param requestStS        The request to the SPARQL-to-SQL endpoint.
     * @param requestBlazegraph The request to the Blazegraph endpoint.
     */
    private void sendRequestAndCompareResults(HttpRequest requestStS, HttpRequest requestBlazegraph) {
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> responseStS = client.send(requestStS, HttpResponse.BodyHandlers.ofString());
            HttpResponse<String> responseBlazegraph = client.send(requestBlazegraph, HttpResponse.BodyHandlers.ofString());

            checkEqualityString(responseStS.body(), responseBlazegraph.body());
        } catch (Exception e) {
            log.error(e.getLocalizedMessage());
        }
    }

    /**
     * Create a HttpRequest object with the URL and the path of the query.
     * @param url The URL of the endpoint.
     * @param path The path of the file containing the query.
     * @return The HttpRequest object.
     * @throws FileNotFoundException If the file is not found.
     */
    private static HttpRequest getHttpRequestByURLandPath(String url, Path path) throws FileNotFoundException {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/sparql-query")
                .header("Accept", "application/sparql-results+json")
                .POST(HttpRequest.BodyPublishers.ofFile(path))
                .build();
    }

    /**
     * Check if the two JSON strings are equal.
     * @param actual The JSON string of the SPARQL-to-SQL endpoint.
     * @param expected The JSON string of the Blazegraph endpoint.
     * @throws JsonProcessingException If the JSON string cannot be processed.
     */
    private void checkEqualityString(String actual, String expected) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootSPARQLtoSQL = objectMapper.readTree(actual);
        JsonNode rootBlazegraph = objectMapper.readTree(expected);

        List<String> varsStS = new ArrayList<>();
        List<String> varsBlazegraph = new ArrayList<>();

        assertEquals(rootBlazegraph.get("head").get("vars").size(), rootSPARQLtoSQL.get("head").get("vars").size());

        rootSPARQLtoSQL.get("head").get("vars").forEach(var -> varsStS.add(var.asText()));
        rootBlazegraph.get("head").get("vars").forEach(var -> varsBlazegraph.add(var.asText()));
        assertTrue(varsStS.containsAll(varsBlazegraph));

        assertEquals(rootBlazegraph.get("results").get("bindings").size(), rootSPARQLtoSQL.get("results").get("bindings").size());
        // TODO : Check if the results are the same
    }
}
