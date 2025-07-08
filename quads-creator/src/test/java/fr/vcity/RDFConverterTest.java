package fr.vcity;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class RDFConverterTest {

    @Test
    void convertToRelational() {
        String inputFile = "dataset-1.ttl";
        String resultFile = inputFile + ".trig";

        RDFConverter rdfConverter = new RDFConverter("relational", "Test", "classpath:", inputFile, "classpath:");
        rdfConverter.convert();

        // Test if the file is created
        String path = Objects.requireNonNull(getClass().getClassLoader().getResource(resultFile)).getPath();
        assertNotNull(path);

        // Compare the result file with the expected file
        String expectedFile = inputFile + "-expected" + ".trig";

        String expectedContent;
        try (BufferedReader expectedReader = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(expectedFile)),
                StandardCharsets.UTF_8))) {
            expectedContent = expectedReader.lines().collect(Collectors.joining("\n"));
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }

        String resultContent;
        try (BufferedReader resultReader = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(resultFile)),
                StandardCharsets.UTF_8))) {
            resultContent = resultReader.lines().collect(Collectors.joining("\n"));
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }

        assertEquals(expectedContent, resultContent);
    }

    @Test
    void convertToTheoretical() {
        String inputFile = "dataset-2.ttl";
        String resultFile = inputFile + ".trig";

        RDFConverter rdfConverter = new RDFConverter("theoretical", "Test-2", "classpath:", inputFile, "classpath:");
        rdfConverter.convert();

        // Test if the file is created
        String path = Objects.requireNonNull(getClass().getClassLoader().getResource(resultFile)).getPath();
        assertNotNull(path);

        // Compare the result file with the expected file
        String expectedFile = inputFile + "-expected" + ".trig";

        String expectedContent;
        try (BufferedReader expectedReader = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(expectedFile)),
                StandardCharsets.UTF_8))) {
            expectedContent = expectedReader.lines().collect(Collectors.joining("\n"));
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }

        String resultContent;
        try (BufferedReader resultReader = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(resultFile)),
                StandardCharsets.UTF_8))) {
            resultContent = resultReader.lines().collect(Collectors.joining("\n"));
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }

        assertEquals(expectedContent, resultContent);
    }
}