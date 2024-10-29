package fr.vcity.converg.controllers;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class QuadsImporterHttpRequestTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ResourceLoader resourceLoader;

    /**
     * Tests the import of the first version and check if the validity is set to 1
     *
     * @throws Exception a input stream or MVC exception
     */
    @Test
    @Order(1)
    void importV1() throws Exception {
        Resource resource = resourceLoader.getResource("classpath:dataset/dataset-1.ttl.trig");

        MockMultipartFile file
                = new MockMultipartFile(
                "file",
                resource.getFilename(),
                MediaType.TEXT_PLAIN_VALUE,
                resource.getInputStream().readAllBytes()
        );

        String url = "http://localhost:" + port + "/import/version";
        HttpHeaders headers = createHeaders();
        HttpEntity<MultiValueMap<String, Object>> requestEntity = getMultiValueMapHttpEntity(file, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        assertEquals("1", response.getBody());
    }

    /**
     * Tests the import of a second version and check the metadata and the data is well annotated
     *
     * @throws Exception a input stream or MVC exception
     */
    @Test
    @Order(2)
    void importV2() throws Exception {
        Resource resource = resourceLoader.getResource("classpath:dataset/dataset-2.ttl.trig");

        MockMultipartFile file
                = new MockMultipartFile(
                "file",
                resource.getFilename(),
                MediaType.TEXT_PLAIN_VALUE,
                resource.getInputStream().readAllBytes()
        );

        String url = "http://localhost:" + port + "/import/version";
        HttpHeaders headers = createHeaders();
        HttpEntity<MultiValueMap<String, Object>> requestEntity = getMultiValueMapHttpEntity(file, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        assertEquals("2", response.getBody());
    }

    /**
     * Tests the import of the metadata
     *
     * @throws Exception a input stream or MVC exception
     */
    @Test
    @Order(3)
    void importMetadata() throws Exception {
        Resource resource = resourceLoader.getResource("classpath:dataset/dataset-1.ttl.trig");

        MockMultipartFile file
                = new MockMultipartFile(
                "file",
                resource.getFilename(),
                MediaType.TEXT_PLAIN_VALUE,
                resource.getInputStream().readAllBytes()
        );

        String url = "http://localhost:" + port + "/import/version";
        HttpHeaders headers = createHeaders();
        HttpEntity<MultiValueMap<String, Object>> requestEntity = getMultiValueMapHttpEntity(file, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @Order(4)
    void removeMetadata() {
        String url = "http://localhost:" + port + "/import/metadata";
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.DELETE, requestEntity, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @Order(5)
    void sendCorruptedFileVersion() throws Exception {
        MockMultipartFile file
                = new MockMultipartFile(
                "file",
                "Corrupted_File.ttl.trig",
                MediaType.TEXT_PLAIN_VALUE,
                new byte[]{0}
        );

        String url = "http://localhost:" + port + "/import/version";
        HttpHeaders headers = createHeaders();
        HttpEntity<MultiValueMap<String, Object>> requestEntity = getMultiValueMapHttpEntity(file, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @Order(6)
    void sendCorruptedFileMetadata() throws Exception {
        MockMultipartFile file
                = new MockMultipartFile(
                "file",
                "Corrupted_File.ttl.trig",
                MediaType.TEXT_PLAIN_VALUE,
                new byte[]{0, 1}
        );

        String url = "http://localhost:" + port + "/import/metadata";
        HttpHeaders headers = createHeaders();
        HttpEntity<MultiValueMap<String, Object>> requestEntity = getMultiValueMapHttpEntity(file, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return headers;
    }

    private static HttpEntity<MultiValueMap<String, Object>> getMultiValueMapHttpEntity(MockMultipartFile file, HttpHeaders headers) throws IOException {
        return new HttpEntity<>(new LinkedMultiValueMap<>() {{
            add("file", new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            });
        }}, headers);
    }
}
