package fr.cnrs.liris.jpugetgil.sparqltosql;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ConnectException;
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
class AppTest {
    private static final Logger log = LoggerFactory.getLogger(AppTest.class);

    @Order(1)
    @ParameterizedTest
    @ValueSource(strings = {"0", "1", "2", "3", "4", "5", "6"})
    void querySPARQLN(String queryNumber) throws Exception {
        log.info("Query number : " + queryNumber);
        Path pathSts = Path.of("src/test/resources/queries/sparql/sts/sts-" + queryNumber + ".rq");
        Path pathBlazegraph = Path.of("src/test/resources/queries/sparql/blazegraph/blazegraph-" + queryNumber + ".rq");
        HttpRequest requestStS = getHttpRequestByURLandPath("http://localhost:8081/rdf/query", pathSts);
        HttpRequest requestBlazegraph = getHttpRequestByURLandPath("http://localhost:9999/blazegraph/namespace/kb/sparql", pathBlazegraph);

        sendRequestAndCompareResults(requestStS, requestBlazegraph);
    }

    /**
     * Send the request to the two endpoints and compare the results.
     *
     * @param requestStS        The request to the quads-query endpoint.
     * @param requestBlazegraph The request to the Blazegraph endpoint.
     */
    private void sendRequestAndCompareResults(HttpRequest requestStS, HttpRequest requestBlazegraph) {
        try (HttpClient client = HttpClient.newHttpClient()) {
            try {
                HttpResponse<String> responseStS = client.send(requestStS, HttpResponse.BodyHandlers.ofString());
                HttpResponse<String> responseBlazegraph = client.send(requestBlazegraph, HttpResponse.BodyHandlers.ofString());

                checkEqualityString(responseStS.body(), responseBlazegraph.body());
            } catch (ConnectException e) {
                throw new RuntimeException("Connection refused. Please check if the endpoints are running.");
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        } catch (UncheckedIOException e) {
            log.error(e.getLocalizedMessage());
        }
    }

    /**
     * Create a HttpRequest object with the URL and the path of the query.
     *
     * @param url  The URL of the endpoint.
     * @param path The path of the file containing the query.
     * @return The HttpRequest object.
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
     *
     * @param actual   The JSON string of the quads-query endpoint.
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

        rootSPARQLtoSQL.get("head").get("vars").forEach(variable -> varsStS.add(variable.asText()));
        rootBlazegraph.get("head").get("vars").forEach(variable -> varsBlazegraph.add(variable.asText()));
        assertTrue(varsStS.containsAll(varsBlazegraph));

        assertEquals(rootBlazegraph.get("results").get("bindings").size(), rootSPARQLtoSQL.get("results").get("bindings").size());
        // TODO : Check if the results are the same
    }
}
