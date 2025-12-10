package fr.cnrs.liris.jpugetgil.converg;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestMethodOrder;
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
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class QuadsQueryAppIT {
    private static final Logger log = LoggerFactory.getLogger(QuadsQueryAppIT.class);

    @Tag("condensed")
    @ParameterizedTest
    @ValueSource(strings = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"})
    void querySPARQLNCardinality(String queryNumber) throws Exception {
        log.info("Query number : {}", queryNumber);
        Path pathSts = Path.of("src/test/resources/queries/sts/sts-" + queryNumber + ".rq");
        Path pathBlazegraph = Path.of("src/test/resources/queries/blazegraph/blazegraph-" + queryNumber + ".rq");
        HttpRequest requestStS = getHttpRequestByURLandPath("http://localhost:8081/rdf/query", pathSts);
        HttpRequest requestBlazegraph = getHttpRequestByURLandPath("http://localhost:9999/blazegraph/namespace/kb/sparql", pathBlazegraph);

        sendRequestAndCompareResultsCardinality(requestStS, requestBlazegraph);
    }

    @Tag("condensed")
    @ParameterizedTest
    @ValueSource(strings = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"})
    void querySPARQLNContent(String queryNumber) throws Exception {
        log.info("Query number : {}", queryNumber);
        Path pathSts = Path.of("src/test/resources/queries/sts/sts-" + queryNumber + ".rq");
        Path pathBlazegraph = Path.of("src/test/resources/queries/blazegraph/blazegraph-" + queryNumber + ".rq");
        HttpRequest requestStS = getHttpRequestByURLandPath("http://localhost:8081/rdf/query", pathSts);
        HttpRequest requestBlazegraph = getHttpRequestByURLandPath("http://localhost:9999/blazegraph/namespace/kb/sparql", pathBlazegraph);

        sendRequestAndCompareResultsContent(requestStS, requestBlazegraph);
    }


    @Tag("flat")
    @ParameterizedTest
    @ValueSource(strings = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"})
    void querySPARQLNCardinalityFlat(String queryNumber) throws Exception {
        log.info("Query number : {}", queryNumber);
        Path pathSts = Path.of("src/test/resources/queries/sts/sts-" + queryNumber + ".rq");
        Path pathBlazegraph = Path.of("src/test/resources/queries/blazegraph/blazegraph-" + queryNumber + ".rq");
        HttpRequest requestStSFlat = getHttpRequestByURLandPath("http://localhost:8082/rdf/query", pathSts);
        HttpRequest requestBlazegraph = getHttpRequestByURLandPath("http://localhost:9999/blazegraph/namespace/kb/sparql", pathBlazegraph);

        sendRequestAndCompareResultsCardinality(requestStSFlat, requestBlazegraph);
    }

    @Tag("flat")
    @ParameterizedTest
    @ValueSource(strings = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"})
    void querySPARQLNContentFlat(String queryNumber) throws Exception {
        log.info("Query number : {}", queryNumber);
        Path pathSts = Path.of("src/test/resources/queries/sts/sts-" + queryNumber + ".rq");
        Path pathBlazegraph = Path.of("src/test/resources/queries/blazegraph/blazegraph-" + queryNumber + ".rq");
        HttpRequest requestStSFlat = getHttpRequestByURLandPath("http://localhost:8082/rdf/query", pathSts);
        HttpRequest requestBlazegraph = getHttpRequestByURLandPath("http://localhost:9999/blazegraph/namespace/kb/sparql", pathBlazegraph);

        sendRequestAndCompareResultsContent(requestStSFlat, requestBlazegraph);
    }

    /**
     * Send the request to the two endpoints and compare the results cardinality.
     *
     * @param requestStS        The request to the quads-query endpoint.
     * @param requestBlazegraph The request to the Blazegraph endpoint.
     */
    private void sendRequestAndCompareResultsCardinality(HttpRequest requestStS, HttpRequest requestBlazegraph) {
        try (HttpClient client = HttpClient.newHttpClient()) {
            try {
                HttpResponse<String> responseStS = client.send(requestStS, HttpResponse.BodyHandlers.ofString());
                HttpResponse<String> responseBlazegraph = client.send(requestBlazegraph, HttpResponse.BodyHandlers.ofString());

                checkCardinalityEqualityString(responseStS.body(), responseBlazegraph.body());
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
     * Send the request to the two endpoints and compare the results content.
     *
     * @param requestStS        The request to the quads-query endpoint.
     * @param requestBlazegraph The request to the Blazegraph endpoint.
     */
    private void sendRequestAndCompareResultsContent(HttpRequest requestStS, HttpRequest requestBlazegraph) {
        try (HttpClient client = HttpClient.newHttpClient()) {
            try {
                HttpResponse<String> responseStS = client.send(requestStS, HttpResponse.BodyHandlers.ofString());
                HttpResponse<String> responseBlazegraph = client.send(requestBlazegraph, HttpResponse.BodyHandlers.ofString());

                checkContentEqualityString(responseStS.body(), responseBlazegraph.body());
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
    private void checkContentEqualityString(String actual, String expected) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootQuadsQuery = objectMapper.readTree(actual);
        JsonNode rootBlazegraph = objectMapper.readTree(expected);

        List<String> varsQuadsQuery = new ArrayList<>();
        List<String> varsBlazegraph = new ArrayList<>();

        rootQuadsQuery.get("head").get("vars").forEach(variable -> varsQuadsQuery.add(variable.asText()));
        rootBlazegraph.get("head").get("vars").forEach(variable -> varsBlazegraph.add(variable.asText()));
        assertTrue(varsQuadsQuery.containsAll(varsBlazegraph));

        JsonNode bgJSONNode = rootBlazegraph.get("results").get("bindings");
        JsonNode quadsQueryJSONNode = rootQuadsQuery.get("results").get("bindings");

        isAllJsonNodesMatched(bgJSONNode, quadsQueryJSONNode);
        isAllJsonNodesMatched(quadsQueryJSONNode, bgJSONNode);
    }

    private void isAllJsonNodesMatched(JsonNode jsonNodeMatchers, JsonNode jsonNode) {
        for (int i = 0; i < jsonNodeMatchers.size(); i++) {
            assertTrue(hasMatchingJSONNode(jsonNode, jsonNodeMatchers.get(i)));
        }
    }

    /**
     * Check if the two JSON cardinality are equal.
     *
     * @param actual   The JSON string of the quads-query endpoint.
     * @param expected The JSON string of the Blazegraph endpoint.
     * @throws JsonProcessingException If the JSON string cannot be processed.
     */
    private void checkCardinalityEqualityString(String actual, String expected) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootQuadsQuery = objectMapper.readTree(actual);
        JsonNode rootBlazegraph = objectMapper.readTree(expected);

        assertEquals(rootBlazegraph.get("head").get("vars").size(), rootQuadsQuery.get("head").get("vars").size());

        JsonNode bgJSONNode = rootBlazegraph.get("results").get("bindings");
        JsonNode quadsQueryJSONNode = rootQuadsQuery.get("results").get("bindings");

        assertEquals(bgJSONNode.size(), quadsQueryJSONNode.size());
    }

    private boolean hasMatchingJSONNode(JsonNode jsonArray, JsonNode matcher) {
        for (int i = 0; i < jsonArray.size(); i++) {
            JsonNode jsonNode = jsonArray.get(i);

            JsonNodeComparator jsonNodeComparator = new JsonNodeComparator();

            if (jsonNodeComparator.compare(jsonNode, matcher) == 0) {
                return true;
            }
        }
        log.error("{} is not found in the JSON array.", matcher);
        return false;
    }
}
