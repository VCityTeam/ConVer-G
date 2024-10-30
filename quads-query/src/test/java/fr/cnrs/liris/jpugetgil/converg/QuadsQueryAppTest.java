package fr.cnrs.liris.jpugetgil.converg;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import jakarta.json.Json;
import org.apache.jena.atlas.json.JSON;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test for SPARQL queries.
 */
class QuadsQueryAppTest {
    private static final Logger log = LoggerFactory.getLogger(QuadsQueryAppTest.class);

    @Order(1)
    @ParameterizedTest
    @ValueSource(strings = {"0", "1", "2", "3", "5", "6", "7", "8", "9", "10", "11"})
    void querySPARQLN(String queryNumber) throws Exception {
        log.info("Query number : " + queryNumber);
        Path pathSts = Path.of("src/test/resources/queries/sts/sts-" + queryNumber + ".rq");
        Path pathBlazegraph = Path.of("src/test/resources/queries/blazegraph/blazegraph-" + queryNumber + ".rq");
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
        JsonNode rootQuadsQuery = objectMapper.readTree(actual);
        JsonNode rootBlazegraph = objectMapper.readTree(expected);

        List<String> varsQuadsQuery = new ArrayList<>();
        List<String> varsBlazegraph = new ArrayList<>();

        assertEquals(rootBlazegraph.get("head").get("vars").size(), rootQuadsQuery.get("head").get("vars").size());

        rootQuadsQuery.get("head").get("vars").forEach(variable -> varsQuadsQuery.add(variable.asText()));
        rootBlazegraph.get("head").get("vars").forEach(variable -> varsBlazegraph.add(variable.asText()));
        assertTrue(varsQuadsQuery.containsAll(varsBlazegraph));

        JsonNode bgJSONNode = rootBlazegraph.get("results").get("bindings");
        JsonNode quadsQueryJSONNode = rootQuadsQuery.get("results").get("bindings");

        assertEquals(bgJSONNode.size(), quadsQueryJSONNode.size());

        for (int i = 0; i < quadsQueryJSONNode.size(); i++) {
                assertTrue(findMatchingJSONNode(bgJSONNode, bgJSONNode.get(i)));
        }
    }

    private boolean findMatchingJSONNode(JsonNode json, JsonNode matcher) {
        for (int i = 0; i < json.size(); i++) {
            if (json.get(i).equals(matcher)) {
                return true;
            }
        }
        return false;
    }
}
