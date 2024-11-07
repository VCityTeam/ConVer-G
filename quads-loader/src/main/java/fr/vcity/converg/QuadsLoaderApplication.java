package fr.vcity.converg;

import io.prometheus.metrics.exporter.httpserver.HTTPServer;
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;

@SpringBootApplication
public class QuadsLoaderApplication {

    public static void main(String[] args) throws IOException {
        SpringApplication.run(QuadsLoaderApplication.class, args);

        JvmMetrics.builder().register();

        HTTPServer server = HTTPServer.builder()
                .port(9400)
                .buildAndStart();

        System.out.println(
                "HTTPServer listening on port:" + server.getPort() + "/metrics");
    }
}
