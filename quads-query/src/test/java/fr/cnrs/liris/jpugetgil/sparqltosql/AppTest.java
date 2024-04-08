package fr.cnrs.liris.jpugetgil.sparqltosql;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

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

    @Test
    @Order(1)
    void querySPARQL0() throws Exception {
        Path path = Path.of("src/test/resources/queries/sparql-0.rq");
        HttpRequest requestStS = getHttpRequestByURLandPath("http://localhost:8081/rdf/query", path);
        HttpRequest requestBlazegraph = getHttpRequestByURLandPath("http://localhost:9999/blazegraph/namespace/kb/sparql", path);

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> responseStS = client.send(requestStS, HttpResponse.BodyHandlers.ofString());
        HttpResponse<String> responseBlazegraph = client.send(requestBlazegraph, HttpResponse.BodyHandlers.ofString());

        checkEqualityString(responseStS.body(), responseBlazegraph.body());
    }

    @Test
    @Order(2)
    void querySPARQL1() throws Exception {
        Path path = Path.of("src/test/resources/queries/sparql-1.rq");
        HttpRequest requestStS = getHttpRequestByURLandPath("http://localhost:8081/rdf/query", path);
        HttpRequest requestBlazegraph = getHttpRequestByURLandPath("http://localhost:9999/blazegraph/namespace/kb/sparql", path);

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> responseStS = client.send(requestStS, HttpResponse.BodyHandlers.ofString());
        HttpResponse<String> responseBlazegraph = client.send(requestBlazegraph, HttpResponse.BodyHandlers.ofString());

        checkEqualityString(responseStS.body(), responseBlazegraph.body());
    }

    @Test
    @Order(3)
    void querySPARQL2() throws Exception {
        Path path = Path.of("src/test/resources/queries/sparql-2.rq");
        HttpRequest requestStS = getHttpRequestByURLandPath("http://localhost:8081/rdf/query", path);
        HttpRequest requestBlazegraph = getHttpRequestByURLandPath("http://localhost:9999/blazegraph/namespace/kb/sparql", path);

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> responseStS = client.send(requestStS, HttpResponse.BodyHandlers.ofString());
        HttpResponse<String> responseBlazegraph = client.send(requestBlazegraph, HttpResponse.BodyHandlers.ofString());

        checkEqualityString(responseStS.body(), responseBlazegraph.body());
    }

    @Test
    @Order(4)
    void querySPARQL3() throws Exception {
        Path path = Path.of("src/test/resources/queries/sparql-3.rq");
        HttpRequest requestStS = getHttpRequestByURLandPath("http://localhost:8081/rdf/query", path);
        HttpRequest requestBlazegraph = getHttpRequestByURLandPath("http://localhost:9999/blazegraph/namespace/kb/sparql", path);

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> responseStS = client.send(requestStS, HttpResponse.BodyHandlers.ofString());
        HttpResponse<String> responseBlazegraph = client.send(requestBlazegraph, HttpResponse.BodyHandlers.ofString());

        checkEqualityString(responseStS.body(), responseBlazegraph.body());
    }

    @Test
    @Order(5)
    void querySPARQL4() throws Exception {
        Path path = Path.of("src/test/resources/queries/sparql-4.rq");
        HttpRequest requestStS = getHttpRequestByURLandPath("http://localhost:8081/rdf/query", path);
        HttpRequest requestBlazegraph = getHttpRequestByURLandPath("http://localhost:9999/blazegraph/namespace/kb/sparql", path);

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> responseStS = client.send(requestStS, HttpResponse.BodyHandlers.ofString());
        HttpResponse<String> responseBlazegraph = client.send(requestBlazegraph, HttpResponse.BodyHandlers.ofString());

        checkEqualityString(responseStS.body(), responseBlazegraph.body());
    }

    @Test
    @Order(6)
    void querySPARQL5() throws Exception {
        Path path = Path.of("src/test/resources/queries/sparql-4.rq");
        HttpRequest requestStS = getHttpRequestByURLandPath("http://localhost:8081/rdf/query", path);
        HttpRequest requestBlazegraph = getHttpRequestByURLandPath("http://localhost:9999/blazegraph/namespace/kb/sparql", path);

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> responseStS = client.send(requestStS, HttpResponse.BodyHandlers.ofString());
        HttpResponse<String> responseBlazegraph = client.send(requestBlazegraph, HttpResponse.BodyHandlers.ofString());

        checkEqualityString(responseStS.body(), responseBlazegraph.body());
    }

    private static HttpRequest getHttpRequestByURLandPath(String url, Path path) throws FileNotFoundException {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/sparql-query")
                .header("Accept", "application/sparql-results+json")
                .POST(HttpRequest.BodyPublishers.ofFile(path))
                .build();
    }

    private void checkEqualityString(String result1, String result2) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootSPARQLtoSQL = objectMapper.readTree(result1);
        JsonNode rootBlazegraph = objectMapper.readTree(result2);

        List<String> varsStS = new ArrayList<>();
        List<String> varsBlazegraph = new ArrayList<>();

        assertEquals(rootSPARQLtoSQL.get("head").get("vars").size(), rootBlazegraph.get("head").get("vars").size());

        rootSPARQLtoSQL.get("head").get("vars").forEach(var -> varsStS.add(var.asText()));
        rootBlazegraph.get("head").get("vars").forEach(var -> varsBlazegraph.add(var.asText()));
        assertTrue(varsStS.containsAll(varsBlazegraph));

        assertEquals(rootBlazegraph.get("results").get("bindings").size(), rootSPARQLtoSQL.get("results").get("bindings").size());
        // TODO : Check if the results are the same
    }
}
